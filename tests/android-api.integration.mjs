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
 assert.equal((await send('/api/android/history','POST',{rounds:[{...game,revision:-1}]})).status,400);
 assert.equal((await send('/api/android/history','POST',{rounds:[{...game,playedAt:'yesterday'}]})).status,400);
 assert.equal((await fetch(base+'/api/android/history')).status,401);
 const account=await (await send('/api/android/account')).json();assert.equal(account.authenticated,true);assert.equal(account.github,false);
 console.log('Android API passed: edits, timestamps, stale-device protection, trash/restore, old-client compatibility, validation, authentication.');
}finally{if(user){await db.handDevice.deleteMany({where:{userId:user.id}});await db.handLog.deleteMany({where:{userId:user.id}});await db.user.delete({where:{id:user.id}});}await db.$disconnect();}
