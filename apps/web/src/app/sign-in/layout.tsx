import { PublicHeader } from '@/components/navigation/PublicHeader';
export default function SignInLayout({ children }: { children: React.ReactNode }) {
  return <><PublicHeader /><main id="main-content">{children}</main></>;
}
