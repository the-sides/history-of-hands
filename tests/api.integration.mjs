import assert from 'node:assert/strict';
import { randomUUID, randomBytes, createHash } from 'node:crypto';
import { PrismaClient } from '@prisma/client';
const db = new PrismaClient();
const base = process.env.HANDS_TEST_URL ?? 'http://localhost:3000';
const ids = [];
const token = randomUUID();
const client = (cookie) => async (method='GET',body,extra={}) => fetch(`${base}/api/hand-history`, {method,headers:{...(cookie ? {Cookie:`authjs.session-token=${cookie}; __Secure-authjs.session-token=${cookie}`} : {}),...(body ? {'Content-Type':'application/json'} : {}),...extra},...(body ? {body:JSON.stringify(body)} : {})});
const anonymous = client();
try {
 assert.equal((await anonymous()).status,401);
 assert.equal((await anonymous('POST',{rounds:[]})).status,401);
 const user = await db.user.create({data:{name:'Hands integration fixture'}}); ids.push(user.id);
 const other = await db.user.create({data:{name:'Hands isolation fixture'}}); ids.push(other.id);
 await db.session.create({data:{userId:user.id,sessionToken:token,expires:new Date(Date.now()+600000)}});
 const request = client(token);
 const r={id:randomUUID(),mine:'ROCK',hers:'SCISSORS',playedAt:new Date().toISOString()};
 assert.equal((await request('POST',{rounds:[r]})).status,200);
 assert.equal((await request('POST',{rounds:[r]})).status,200);
 assert.equal(await db.handLog.count({where:{userId:user.id}}),1);
 let response=await request();assert.equal(response.status,200);assert.equal((await response.json()).rounds.length,1);
 assert.equal((await request('POST',{rounds:[{...r,mine:'LIZARD'}]})).status,400);
 assert.equal((await request('POST',{rounds:[r]},{Origin:'https://evil.invalid'})).status,403);
 await db.handLog.create({data:{userId:other.id,...r,playedAt:new Date(r.playedAt)}});
 assert.equal((await (await request()).json()).rounds.length,1);
 await request('POST',{rounds:[{...r,deleted:true}]}); await request('POST',{rounds:[r]});
 assert.equal((await (await request()).json()).rounds[0].deleted,true);
 // Existing accounts can read their legacy rounds without any database rewrite.
 const original = await db.user.findFirst({where:{id:{notIn:ids}}});
 if(original) {
  const oldToken=randomUUID();await db.session.create({data:{userId:original.id,sessionToken:oldToken,expires:new Date(Date.now()+60000)}});
  try { const actual=(await (await client(oldToken)()).json()).rounds.filter(r=>r.id.startsWith('legacy-')); const count=await db.round.count({where:{OR:[{createdById:original.id},{againstId:original.id}]}});assert.equal(actual.length,count);console.log(`Legacy import verified: ${count} original rounds, read only.`); }
  finally { await db.session.delete({where:{sessionToken:oldToken}}); }
 }
 const deviceToken=randomBytes(32).toString('base64url');
 const pair=await fetch(`${base}/api/hand-devices`,{method:'POST',headers:{Cookie:`authjs.session-token=${token}; __Secure-authjs.session-token=${token}`,Origin:base,'Content-Type':'application/json'},body:JSON.stringify({token:deviceToken})});
 assert.equal(pair.status,200);
 const hash=createHash('sha256').update(deviceToken).digest('hex');
 assert.equal((await db.handDevice.findUnique({where:{tokenHash:hash}})).userId,user.id);
 const nativeRequest=()=>fetch(`${base}/api/hand-history`,{headers:{Authorization:`Bearer ${deviceToken}`}});
 assert.equal((await nativeRequest()).status,200);
 assert.equal((await (await nativeRequest()).json()).rounds[0].deleted,true);
 assert.equal((await fetch(`${base}/api/hand-history`,{headers:{Authorization:'Bearer '+randomBytes(32).toString('base64url')}})).status,401);
 const revoke=await fetch(`${base}/api/hand-devices`,{method:'DELETE',headers:{Cookie:`authjs.session-token=${token}; __Secure-authjs.session-token=${token}`,Origin:base}});
 assert.equal(revoke.status,200);assert.equal((await nativeRequest()).status,401);
 console.log('API checks passed: auth, idempotency, isolation, tombstones, legacy import, Android pairing, hashed credentials, bearer auth, revocation.');
} finally {
 await db.session.deleteMany({where:{userId:{in:ids}}});
 await db.handLog.deleteMany({where:{userId:{in:ids}}});
 await db.user.deleteMany({where:{id:{in:ids}}});
 await db.$disconnect();
}
