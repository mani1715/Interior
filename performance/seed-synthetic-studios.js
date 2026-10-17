const { execFileSync } = require('child_process');

function seedSyntheticStudios() {
  const psql = 'C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe';
  const env = { ...process.env, PGPASSWORD: 'postgres' };

  // 1. Get dev.designer user ID
  const sqlGetUserId = "SELECT id FROM users WHERE email = 'dev.designer@platform.local';";
  const userId = execFileSync(psql, [
    '-h', 'localhost', '-p', '5433', '-U', 'postgres', '-d', 'interior_design_dev',
    '-t', '-A', '-c', sqlGetUserId
  ], { env, encoding: 'utf8' }).trim();

  if (!userId) {
    console.error('User dev.designer@platform.local not found! Please run dev login first.');
    return [];
  }

  console.log(`Found dev.designer user ID: ${userId}`);

  // 2. Query or create 10 synthetic studios
  const studios = [];
  for (let i = 1; i <= 10; i++) {
    const slug = `dev-studio-synthetic-${i}`;
    const name = `Synthetic Studio ${i}`;

    // Check if exists
    let studioId = execFileSync(psql, [
      '-h', 'localhost', '-p', '5433', '-U', 'postgres', '-d', 'interior_design_dev',
      '-t', '-A', '-c', `SELECT id FROM designer_studios WHERE slug = '${slug}';`
    ], { env, encoding: 'utf8' }).trim();

    if (!studioId) {
      // Create studio with random UUID
      const createSql = `
        INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
        VALUES (gen_random_uuid(), '${name}', '${slug}', '${userId}', 'ACTIVE', 'PUBLISHED', 'Bengaluru', 'Karnataka', 'IN', now(), now())
        RETURNING id;
      `;
      studioId = execFileSync(psql, [
        '-h', 'localhost', '-p', '5433', '-U', 'postgres', '-d', 'interior_design_dev',
        '-t', '-A', '-c', createSql
      ], { env, encoding: 'utf8' }).trim().split('\n')[0].trim();

      // Add studio member
      const memberSql = `
        INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
        VALUES (gen_random_uuid(), '${studioId}', '${userId}', 'DESIGNER_ADMIN', now())
        ON CONFLICT (studio_id, user_id) DO NOTHING;
      `;
      execFileSync(psql, [
        '-h', 'localhost', '-p', '5433', '-U', 'postgres', '-d', 'interior_design_dev',
        '-c', memberSql
      ], { env, encoding: 'utf8' });

      // Add subscription (STANDARD plan: 01923000-0000-7000-8000-000000000002, PRO plan: 01923000-0000-7000-8000-000000000004)
      const subSql = `
        INSERT INTO studio_subscriptions (id, studio_id, plan_id, status, provider, provider_customer_id, provider_subscription_id, current_period_start, current_period_end, cancel_at_period_end, created_at, updated_at, version)
        VALUES (gen_random_uuid(), '${studioId}', '01923000-0000-7000-8000-000000000004', 'ACTIVE', 'fake', 'cus_${i}', 'sub_${i}', now() - interval '5 days', now() + interval '25 days', false, now(), now(), 1);
      `;
      execFileSync(psql, [
        '-h', 'localhost', '-p', '5433', '-U', 'postgres', '-d', 'interior_design_dev',
        '-c', subSql
      ], { env, encoding: 'utf8' });
    }

    studios.push({ id: studioId, slug, name });
  }

  console.log(`Seeded ${studios.length} synthetic studios.`);
  return studios;
}

if (require.main === module) {
  const s = seedSyntheticStudios();
  console.log('Studios:', s);
}

module.exports = { seedSyntheticStudios };
