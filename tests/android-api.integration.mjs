import assert from 'node:assert/strict';
import {randomBytes,randomUUID,createHash} from 'node:crypto';
import {PrismaClient} from '@prisma/client';
const db=new PrismaClient();const base=process.env.HANDS_TEST_URL??'http://localhost:3001';const token=randomBytes(32).toString('base64url');let user;
const send=(path,method='GET',body)=>fetch(base+path,{method,headers:{Authorization:`Bearer ${token}`,...(body?{'Content-Type':'application/json'}:{})},...(body?{body:JSON.stringify(body)}:{})});
try{
 user=await db.user.create({data:{name:'Hands v2 integration fixture'}});await db.handDevice.create({data:{tokenHash:createHash('sha256').update(token).digest('hex'),userId:user.id}});
 const game={id:randomUUID(),mine:'ROCK',hers:'SCISSORS',playedAt:'2026-10-01T12:00:00.000Z',revision:100};
 assert.equal((await send('/api/android/history','POST',{rounds:[game]})).status,200);
 let data=await (await send('/api/android/history')).json();assert.equal(data.rounds[0].mine,'ROCK');assert.equal(data.rounds[0].revision,100);
 const correction={...game,mine:'PAPER',hers:'ROCK',playedAt:'2026-10-02T14:30:00.000Z',revision:200};await send('/api/android/history','POST',{rounds:[correction]});
 await send('/api/android/history','POST',{rounds:[game]}); // old device / export
 data=await (await send('/api/android/history')).json();assert.equal(data.rounds[0].mine,'PAPER');assert.equal(data.rounds[0].playedAt,correction.playedAt);
 await send('/api/android/history','POST',{rounds:[{...correction,deleted:true,revision:300}]});
 await send('/api/android/history','POST',{rounds:[correction]});data=await (await send('/api/android/history')).json();assert.equal(data.rounds[0].deleted,true);
 await send('/api/android/history','POST',{rounds:[{...correction,deleted:false,revision:400}]});
 // The unchanged website or v1 Android client cannot erase a restored / corrected v2 game.
 await send('/api/hand-history','POST',{rounds:[{...game,deleted:true}]});data=await (await send('/api/android/history')).json();assert.equal(data.rounds[0].deleted,false);assert.equal(data.rounds[0].revision,400);assert.equal(data.rounds[0].mine,'PAPER');
 // Both devices edited revision 400. Only the first can commit, even if the other has a higher clock.
 const first={...correction,mine:'SCISSORS',hers:'PAPER',revision:500,baseRevision:400};
 let result=await (await send('/api/android/history','POST',{rounds:[first]})).json();assert.deepEqual(result.accepted,[game.id]);
 const competing={...correction,mine:'ROCK',hers:'PAPER',deleted:true,revision:900,baseRevision:400};
 result=await (await send('/api/android/history','POST',{rounds:[competing]})).json();assert.equal(result.conflicts[0].revision,500);assert.deepEqual(result.accepted,[]);
 data=await (await send('/api/android/history')).json();assert.equal(data.rounds[0].mine,'SCISSORS');assert.equal(data.rounds[0].deleted,false);
 // Explicit user choice based on latest remote revision can now replace it.
 result=await (await send('/api/android/history','POST',{rounds:[{...competing,baseRevision:500}]})).json();assert.deepEqual(result.accepted,[game.id]);
 // A lost response and retry of the exact accepted record must be idempotent.
 result=await (await send('/api/android/history','POST',{rounds:[{...competing,baseRevision:500}]})).json();assert.deepEqual(result.accepted,[game.id]);
 const racing=[{...correction,revision:1000,baseRevision:900},{...correction,mine:'ROCK',revision:1100,baseRevision:900}];
 const raced=await Promise.all(racing.map(async r=>(await send('/api/android/history','POST',{rounds:[r]})).json()));
 assert.equal(raced.reduce((n,r)=>n+r.accepted.length,0),1);assert.equal(raced.reduce((n,r)=>n+r.conflicts.length,0),1);
 const unrelated={...game,id:randomUUID(),revision:100,baseRevision:-1};
 result=await (await send('/api/android/history','POST',{rounds:[competing,unrelated]})).json();assert.deepEqual(result.accepted,[unrelated.id]);assert.equal(result.conflicts.length,1);
 assert.equal((await send('/api/android/history','POST',{rounds:[{...game,revision:-1}]})).status,400);
 assert.equal((await send('/api/android/history','POST',{rounds:[{...game,playedAt:'yesterday'}]})).status,400);
 assert.equal((await fetch(base+'/api/android/history')).status,401);
 const account=await (await send('/api/android/account')).json();assert.equal(account.authenticated,true);assert.equal(account.github,false);
 console.log('Android API passed: edits, timestamps, stale-device protection, trash/restore, old-client compatibility, validation, authentication.');
}finally{if(user){await db.handDevice.deleteMany({where:{userId:user.id}});await db.handLog.deleteMany({where:{userId:user.id}});await db.user.delete({where:{id:user.id}});}await db.$disconnect();}
