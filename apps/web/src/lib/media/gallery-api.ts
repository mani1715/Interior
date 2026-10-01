import {apiFetch} from '../api-client';
import type {GalleryFolder} from '@/components/portfolio/gallery/PortfolioGallery';
export type StoredFolder={id:string;name:string;description:string;published:boolean;version:number;mediaIds:string[]};
export const fetchGalleryFolders=()=>apiFetch<StoredFolder[]>('/gallery-folders');
export const saveGalleryFolder=(folder:Omit<StoredFolder,'id'>,id?:string)=>apiFetch<StoredFolder>(`/gallery-folders${id?`/${encodeURIComponent(id)}`:''}`,{method:id?'PUT':'POST',body:JSON.stringify(folder)});
export const fetchPublicGallery=(slug:string)=>apiFetch<GalleryFolder[]>(`/public/studios/${encodeURIComponent(slug)}/gallery`);
