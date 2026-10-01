import { PublicHeader } from '@/components/navigation/PublicHeader';
export default function SignUpLayout({ children }: { children: React.ReactNode }) {
  return <><PublicHeader /><main id="main-content">{children}</main></>;
}
