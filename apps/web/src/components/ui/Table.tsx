import React from 'react';

export interface Column<T> {
  header: string;
  accessor: keyof T | ((row: T) => React.ReactNode);
  className?: string;
}

export interface DataTableProps<T> {
  data: T[];
  columns: Column<T>[];
  keyExtractor: (row: T) => string;
  emptyMessage?: string;
  className?: string;
}

export function DataTable<T>({
  data,
  columns,
  keyExtractor,
  emptyMessage = 'No records found',
  className = '',
}: DataTableProps<T>) {
  if (data.length === 0) {
    return (
      <div
        style={{
          padding: 'var(--space-32)',
          textAlign: 'center',
          backgroundColor: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          color: 'var(--text-muted)',
        }}
      >
        {emptyMessage}
      </div>
    );
  }

  return (
    <div style={{ width: '100%', overflowX: 'auto' }} className={`table-container ${className}`}>
      {/* Desktop View: Semantic HTML Table */}
      <table
        style={{
          width: '100%',
          borderCollapse: 'collapse',
          backgroundColor: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          textAlign: 'left',
          fontSize: 'var(--text-body-small)',
        }}
        className="data-table"
      >
        <thead>
          <tr style={{ borderBottom: '2px solid var(--border)', backgroundColor: 'var(--surface-alt)' }}>
            {columns.map((col, idx) => (
              <th
                key={idx}
                scope="col"
                style={{
                  padding: '12px 16px',
                  fontWeight: 600,
                  color: 'var(--text-primary)',
                }}
                className={col.className}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr
              key={keyExtractor(row)}
              style={{
                borderBottom: '1px solid var(--border)',
                transition: 'background-color var(--duration-fast)',
              }}
            >
              {columns.map((col, idx) => {
                const cellValue =
                  typeof col.accessor === 'function' ? col.accessor(row) : (row[col.accessor] as React.ReactNode);
                return (
                  <td key={idx} style={{ padding: '14px 16px' }} className={col.className}>
                    {cellValue}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
