import React, { useEffect, useMemo, useState } from 'react';
import { Table, Select, Button, Typography, Spin, Alert, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useGetAdminUsersQuery, useUpdateUserRoleMutation } from '../authApi';
import { useAuth } from '../../../shared/hooks/useAuth';
import { Role, ROLE_LABELS } from '../../../constants/roles';
import '../../../App.css';

const ROLE_OPTIONS = (Object.keys(ROLE_LABELS) as Role[]).map((r) => ({
  value: r,
  label: ROLE_LABELS[r],
}));

const AdminUsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const { data, isLoading, isError, error, refetch } = useGetAdminUsersQuery();
  const [updateRole, { isLoading: saving }] = useUpdateUserRoleMutation();

  const users = data?.data ?? [];
  const [draftRoleById, setDraftRoleById] = useState<Record<string, Role>>({});

  useEffect(() => {
    if (!users.length) return;
    setDraftRoleById((prev) => {
      const next = { ...prev };
      for (const u of users) {
        if (next[u.id] === undefined) next[u.id] = u.role;
      }
      return next;
    });
  }, [users]);

  const handleSave = async (userId: string) => {
    const role = draftRoleById[userId];
    if (!role) return;
    try {
      const res = await updateRole({ userId, role }).unwrap();
      if (res.success) message.success('Role updated.');
    } catch (e: unknown) {
      const msg =
        e && typeof e === 'object' && 'data' in e
          ? (e as { data?: { message?: string } }).data?.message
          : undefined;
      message.error(msg ?? 'Could not update role.');
      void refetch();
    }
  };

  const columns: ColumnsType<(typeof users)[0]> = useMemo(
    () => [
      { title: 'Username', dataIndex: 'username', key: 'username' },
      { title: 'Email', dataIndex: 'email', key: 'email', ellipsis: true },
      {
        title: 'Role',
        key: 'role',
        render: (_, record) => {
          const isSelf = currentUser?.id === record.id;
          const draft = draftRoleById[record.id] ?? record.role;
          const unchanged = draft === record.role;
          return (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
              <Select
                style={{ minWidth: 160 }}
                value={draft}
                disabled={isSelf}
                options={ROLE_OPTIONS}
                onChange={(v) =>
                  setDraftRoleById((m) => ({ ...m, [record.id]: v as Role }))
                }
              />
              <Button
                type="primary"
                size="small"
                loading={saving}
                disabled={isSelf || unchanged}
                onClick={() => void handleSave(record.id)}
              >
                Save
              </Button>
              {isSelf && (
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  You cannot change your own role
                </Typography.Text>
              )}
            </div>
          );
        },
      },
    ],
    [currentUser?.id, draftRoleById, saving]
  );

  if (isLoading) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError) {
    return (
      <div style={{ maxWidth: 560, margin: '2rem auto', padding: '0 1rem' }}>
        <Alert
          type="error"
          showIcon
          message="Could not load users"
          description={
            (error as { data?: { message?: string } })?.data?.message ??
            'You may need administrator access.'
          }
        />
      </div>
    );
  }

  return (
    <div
      style={{
        fontFamily: "'IBM Plex Sans', sans-serif",
        minHeight: '100vh',
        background: '#f9f9f8',
        padding: '2rem',
      }}
    >
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        <Typography.Title level={3} style={{ marginBottom: 8 }}>
          User roles
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 24 }}>
          Assign roles for each account. Only administrators can access this page.
        </Typography.Paragraph>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={users}
          pagination={false}
          locale={{ emptyText: 'No users found' }}
        />
        <div style={{ marginTop: 24 }}>
          <a href="/dashboard" style={{ color: '#2563eb' }}>
            ← Back to dashboard
          </a>
        </div>
      </div>
    </div>
  );
};

export default AdminUsersPage;
