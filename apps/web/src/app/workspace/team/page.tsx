import { Metadata } from 'next';
import { TeamWorkspace } from '@/components/team/TeamWorkspace';

export const metadata: Metadata = {
  title: 'Team & Members | Studio Workspace',
  description: 'Manage studio designers, invitations, roles, and collaborative permissions.',
};

export default function WorkspaceTeamPage() {
  return <TeamWorkspace />;
}
