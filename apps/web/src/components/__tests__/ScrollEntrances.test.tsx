import React from 'react';
import { act, cleanup, render } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { ScrollEntrances } from '../home/ScrollEntrances';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });
it('reveals offscreen content on entry and clears all motion when reduced motion is enabled', () => {
  let intersect: IntersectionObserverCallback = () => {};
  let change = () => {};
  const media = {matches:false,addEventListener:(_:string,callback:()=>void)=>{change=callback;},removeEventListener:vi.fn()};
  vi.stubGlobal('matchMedia',()=>media);
  vi.stubGlobal('IntersectionObserver',class { constructor(callback:IntersectionObserverCallback){intersect=callback;} observe(){} unobserve(){} disconnect(){} });
  vi.spyOn(HTMLElement.prototype,'getBoundingClientRect').mockReturnValue({top:2000,bottom:2100,height:100,width:100,left:0,right:100,x:0,y:2000,toJSON:()=>({})});
  const {container}=render(<ScrollEntrances><h2>Discover interiors</h2><div data-motion-image="right">Photo</div></ScrollEntrances>);
  const heading=container.querySelector('h2')!;
  expect(heading.dataset.entered).toBeUndefined();
  act(()=>intersect([{target:heading,isIntersecting:true} as unknown as IntersectionObserverEntry],{} as IntersectionObserver));
  expect(heading.dataset.entered).toBe('true');
  expect(container.querySelector('[data-motion-image]')?.getAttribute('data-enter')).toBe('right');
  act(()=>{media.matches=true;change();});
  expect(container.querySelector('[data-entrances]')).toBeNull();
  expect(container.querySelector('[data-enter]')).toBeNull();
});
it('retains readable server content when animation APIs are unavailable',()=>{
  vi.stubGlobal('matchMedia',undefined);
  const {container}=render(<ScrollEntrances><h2>Discover interiors</h2></ScrollEntrances>);
  expect(container.querySelector('h2')?.textContent).toBe('Discover interiors');
  expect(container.querySelector('[data-entrances]')).toBeNull();
});
