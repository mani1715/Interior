'use client';
import {useEffect,useState} from 'react';
import {fetchPublicGallery} from '@/lib/media/gallery-api';
import {PortfolioGallery,type GalleryFolder} from './PortfolioGallery';
export function PublicPortfolioGallery({studioSlug}:{studioSlug:string}){
  const [folders,setFolders]=useState<GalleryFolder[]>([]);
  useEffect(()=>{let active=true;fetchPublicGallery(studioSlug).then(data=>{if(active)setFolders(data);}).catch(()=>{if(active)setFolders([]);});return()=>{active=false;};},[studioSlug]);
  return <PortfolioGallery folders={folders}/>;
}
