const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const ts=require('typescript');
// Test the actual API client in isolation; never load workspace environment files.
test('demo account requests cannot silently reuse the normal login cookie',async()=>{
 const directory=fs.mkdtempSync(path.join(os.tmpdir(),'shop-demo-api-test-'));
 let active=true,calls=0;
 const prior={window:global.window,sessionStorage:global.sessionStorage,fetch:global.fetch};
 try{
  for(const name of ['api','demo','shop'])fs.writeFileSync(path.join(directory,name+'.js'),ts.transpileModule(fs.readFileSync(path.join(__dirname,'../lib',name+'.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText);
  global.window={};global.sessionStorage={getItem:()=>active?'demo_fixture':null};
  global.fetch=async()=>{calls++;return{ok:true,json:async()=>[]};};
  const {api,getCsrfToken}=require(path.join(directory,'api.js'));
  for(const action of [api.getMySessions,api.getAdminSessions,api.getAdminOrders,getCsrfToken,()=>api.kickSession(1),()=>api.adminKickSession(1)])await assert.rejects(action,/Exit demo/);
  assert.equal(calls,0);
  active=false;assert.deepEqual(await api.getMySessions(),[]);assert.equal(calls,1);
 }finally{Object.assign(global,prior);fs.rmSync(directory,{recursive:true,force:true});}
});
