// Theme controller and palette checks use Node's built-in runner; no test dependency.
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../public/theme.js',import.meta.url),'utf8');
function setup(saved=null,blocked=false){
  const values=new Map(saved===null?[]:[['careerup.theme',saved]]),listeners={},events=[];
  const meta={content:''};let writes=0;
  const window={document:{documentElement:{dataset:{}},querySelector:()=>meta},
    localStorage:{getItem:key=>{if(blocked)throw Error('blocked');return values.get(key)||null;},
      setItem:(key,value)=>{if(blocked)throw Error('blocked');writes++;values.set(key,value);}},
    Event:class{constructor(type){this.type=type;}},
    dispatchEvent:event=>{events.push(event.type);listeners[event.type]?.(event);},
    addEventListener:(name,fn)=>listeners[name]=fn};
  vm.runInNewContext(source,{window});
  return {window,values,listeners,events,meta,writes:()=>writes};
}
test('dark is the default and initialization does not overwrite preferences',()=>{
  const s=setup();assert.equal(s.window.CareerUpTheme.get(),'dark');assert.equal(s.writes(),0);
  assert.equal(s.meta.content,'#0f0f1a');
});
test('saved light is applied before the application mounts',()=>{
  const s=setup('light');assert.equal(s.window.document.documentElement.dataset.theme,'light');
  assert.equal(s.meta.content,'#f7f7fa');assert.equal(s.writes(),0);
});
test('invalid stored preference falls back to dark',()=>assert.equal(setup('purple').window.CareerUpTheme.get(),'dark'));
test('switching is immediate, persisted and reversible',()=>{
  const s=setup();s.window.CareerUpTheme.toggle();assert.equal(s.window.CareerUpTheme.get(),'light');
  assert.equal(s.values.get('careerup.theme'),'light');assert.equal(s.meta.content,'#f7f7fa');
  s.window.CareerUpTheme.toggle();assert.equal(s.window.CareerUpTheme.get(),'dark');assert.equal(s.writes(),2);
  assert.deepEqual(s.events,['careerup:theme','careerup:theme','careerup:theme']);
});
test('blocked storage still permits switching for the current page',()=>{
  const s=setup(null,true);s.window.CareerUpTheme.toggle();assert.equal(s.window.CareerUpTheme.get(),'light');
});
test('a second tab and clearing storage update appearance without writing back',()=>{
  const s=setup();s.values.set('careerup.theme','light');s.listeners.storage({key:'careerup.theme'});
  assert.equal(s.window.CareerUpTheme.get(),'light');assert.equal(s.writes(),0);
  s.values.clear();s.listeners.storage({key:null});assert.equal(s.window.CareerUpTheme.get(),'dark');
});
test('unrelated browser storage changes do not affect the theme',()=>{
  const s=setup();s.values.set('careerup.theme','light');s.listeners.storage({key:'unrelated'});
  assert.equal(s.window.CareerUpTheme.get(),'dark');
});
test('startup theme script precedes React and remains nonblocking for saved choices',()=>{
  const html=readFileSync(new URL('../index.html',import.meta.url),'utf8');
  assert.ok(html.indexOf('src="/theme.js"')<html.indexOf('src="/src/main.jsx"'));
  assert.ok(!/fetch\(|XMLHttpRequest|location\.|setInterval|setTimeout/.test(source));
});
function luminance(hex){
  const rgb=hex.slice(1).match(/../g).map(v=>parseInt(v,16)/255).map(v=>v<=0.04045?v/12.92:((v+0.055)/1.055)**2.4);
  return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722;
}
function contrast(a,b){const x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
test('light palette text pairs meet 4.5 to 1 contrast',()=>{
  const css=readFileSync(new URL('../src/styles.css',import.meta.url),'utf8');
  const palette=css.match(/:root\[data-theme="light"\]\s*\{([^}]+)\}/)[1];
  const color=name=>palette.match(new RegExp('--'+name+':\\s*(#[a-f0-9]{6})'))[1];
  for(const surface of [color('bg'),color('card')])for(const text of [color('text'),color('muted')])assert.ok(contrast(text,surface)>=4.5,`${text} on ${surface}`);
  for(const [text,bg] of [['#5b21b6','#f0eafa'],['#166534','#eaf7ee'],['#9f1239','#fff0f3'],['#854d0e','#fff7df'],['#ffffff','#4f46e5'],['#ffffff','#8b4fec']])assert.ok(contrast(text,bg)>=4.5,`${text} on ${bg}`);
});
test('theme rules do not invert or filter uploaded media',()=>{
  const css=readFileSync(new URL('../src/styles.css',import.meta.url),'utf8').split('/* 10. LIGHT / DARK APPEARANCE')[1];
  assert.ok(!/filter\s*:|mix-blend-mode\s*:/.test(css));
});
