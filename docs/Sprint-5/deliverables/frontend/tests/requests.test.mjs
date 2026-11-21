// Small checks for real request/error helpers; no extra testing dependency.
import test from 'node:test';
import assert from 'node:assert/strict';
import {request,fileProblem,upload,CONNECTION_MESSAGE} from '../src/requests.js';

test('supported files and exact size boundaries',()=>{
  for(const name of ['a.JPG','a.jpeg','a.png','a.gif','a.mp4','a.webm'])assert.equal(fileProblem({name,size:100}),'');
  assert.equal(fileProblem({name:'a.png',size:10*1024*1024}),'');
  assert.equal(fileProblem({name:'a.mp4',size:50*1024*1024}),'');
});
test('missing, empty, unsupported and oversized files have useful prompts',()=>{
  assert.match(fileProblem(null),/Choose/);
  assert.match(fileProblem({name:'a.png',size:0}),/empty/);
  assert.match(fileProblem({name:'a.mov',size:100}),/Convert/);
  assert.match(fileProblem({name:'a.png',size:10*1024*1024+1}),/too large/);
  assert.match(fileProblem({name:'a.mp4',size:50*1024*1024+1}),/too large/);
});
test('JSON success and empty responses',async()=>{
  globalThis.fetch=async()=>({status:200,ok:true,json:async()=>({id:1})});
  assert.deepEqual(await request('/test'),{id:1});
  globalThis.fetch=async()=>({status:204});assert.equal(await request('/test'),null);
});
test('network and server exceptions are never shown raw',async()=>{
  globalThis.fetch=async()=>{throw new Error('ECONNREFUSED');};
  await assert.rejects(request('/test'),{message:CONNECTION_MESSAGE});
  globalThis.fetch=async()=>({status:500,ok:false,json:async()=>({message:'java.sql SQLException jdbc:mysql'})});
  await assert.rejects(request('/test'),e=>e.status===500&&!/java|jdbc/.test(e.message));
});
test('session, validation and malformed responses',async()=>{
  for(const [status,message] of [[401,'Please sign in again.'],[400,'Enter a title.'],[413,'That upload is too large.']]){
    globalThis.fetch=async()=>({status,ok:false,json:async()=>({message})});
    await assert.rejects(request('/test'),e=>e.status===status&&e.message===message);
  }
  globalThis.fetch=async()=>({status:200,ok:true,json:async()=>{throw new Error('invalid JSON');}});
  await assert.rejects(request('/test'),e=>e.status===502);
});
test('native upload reports progress and preserves browser multipart headers',async()=>{
  let progress=0;
  globalThis.XMLHttpRequest=class{
    upload={};status=200;responseText='{"id":7}';
    open(method,path){assert.equal(method,'POST');assert.equal(path,'/api/student/posts/upload');}
    setRequestHeader(name){assert.notEqual(name.toLowerCase(),'content-type');}
    send(){this.upload.onprogress({lengthComputable:true,loaded:50,total:100});this.onload();}
  };
  assert.deepEqual(await upload('/student/posts/upload',{}, {'Idempotency-Key':'example'},p=>progress=p),{id:7});
  assert.equal(progress,50);
});
test('upload network, timeout and interruption errors are readable',async()=>{
  for(const [event,pattern] of [['onerror',/could not connect/],['ontimeout',/took too long/],['onabort',/interrupted/]]){
    globalThis.XMLHttpRequest=class{upload={};open(){}setRequestHeader(){}send(){this[event]();}};
    await assert.rejects(upload('/test',{}, {},()=>{}),pattern);
  }
});
