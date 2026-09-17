'use client';

import React, { forwardRef, useId } from 'react';
import { Search, AlertCircle } from 'lucide-react';

/* ==========================================================================
   Base Form Field Wrapper (Accessible Label + Helper + Error)
   ========================================================================== */
export interface FormFieldProps {
  label?: string;
  helperText?: string;
  error?: string;
  required?: boolean;
  id?: string;
  children: (id: string, errorId?: string, helperId?: string) => React.ReactNode;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  helperText,
  error,
  required,
  id: customId,
  children,
}) => {
  const autoId = useId();
  const id = customId || autoId;
  const errorId = error ? `${id}-error` : undefined;
  const helperId = helperText ? `${id}-helper` : undefined;

  return (
    <div style={{ marginBottom: 'var(--space-16)', width: '100%' }}>
      {label && (
        <label
          htmlFor={id}
          style={{
            display: 'block',
            fontSize: 'var(--text-body-small)',
            fontWeight: 600,
            marginBottom: '6px',
            color: 'var(--text-primary)',
          }}
        >
          {label} {required && <span style={{ color: 'var(--danger)' }}>*</span>}
        </label>
      )}
      {children(id, errorId, helperId)}
      {helperText && !error && (
        <p id={helperId} style={{ fontSize: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: '4px' }}>
          {helperText}
        </p>
      )}
      {error && (
        <p
          id={errorId}
          role="alert"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            fontSize: 'var(--text-caption)',
            color: 'var(--danger)',
            marginTop: '4px',
            fontWeight: 500,
          }}
        >
          <AlertCircle size={12} aria-hidden="true" />
          {error}
        </p>
      )}
    </div>
  );
};

/* ==========================================================================
   TextInput Component
   ========================================================================== */
export interface TextInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  helperText?: string;
  error?: string;
  leftAddon?: React.ReactNode;
  rightAddon?: React.ReactNode;
}

export const TextInput = forwardRef<HTMLInputElement, TextInputProps>(
  ({ label, helperText, error, required, leftAddon, rightAddon, className = '', style, ...props }, ref) => {
    return (
      <FormField label={label} helperText={helperText} error={error} required={required} id={props.id}>
        {(id, errorId, helperId) => (
          <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
            {leftAddon && (
              <span style={{ position: 'absolute', left: '12px', color: 'var(--text-muted)', pointerEvents: 'none' }}>
                {leftAddon}
              </span>
            )}
            <input
              ref={ref}
              id={id}
              aria-invalid={!!error}
              aria-describedby={[errorId, helperId].filter(Boolean).join(' ') || undefined}
              style={{
                width: '100%',
                minHeight: 'var(--min-touch-target)', // 44px min height
                padding: '10px 14px',
                paddingLeft: leftAddon ? '38px' : '14px',
                paddingRight: rightAddon ? '38px' : '14px',
                fontFamily: 'var(--font-sans)',
                fontSize: 'var(--text-body)',
                borderRadius: 'var(--radius-sm)',
                border: `1px solid ${error ? 'var(--danger)' : 'var(--border)'}`,
                backgroundColor: 'var(--surface)',
                color: 'var(--text-primary)',
                transition: 'border-color var(--duration-fast)',
                ...style,
              }}
              className={`input-text ${className}`}
              {...props}
            />
            {rightAddon && (
              <span style={{ position: 'absolute', right: '12px', color: 'var(--text-muted)' }}>
                {rightAddon}
              </span>
            )}
          </div>
        )}
      </FormField>
    );
  }
);
TextInput.displayName = 'TextInput';

/* ==========================================================================
   TextArea Component
   ========================================================================== */
export interface TextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  helperText?: string;
  error?: string;
}

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(
  ({ label, helperText, error, required, className = '', style, rows = 4, ...props }, ref) => {
    return (
      <FormField label={label} helperText={helperText} error={error} required={required} id={props.id}>
        {(id, errorId, helperId) => (
          <textarea
            ref={ref}
            id={id}
            rows={rows}
            aria-invalid={!!error}
            aria-describedby={[errorId, helperId].filter(Boolean).join(' ') || undefined}
            style={{
              width: '100%',
              padding: '12px 14px',
              fontFamily: 'var(--font-sans)',
              fontSize: 'var(--text-body)',
              borderRadius: 'var(--radius-sm)',
              border: `1px solid ${error ? 'var(--danger)' : 'var(--border)'}`,
              backgroundColor: 'var(--surface)',
              color: 'var(--text-primary)',
              lineHeight: 'var(--leading-normal)',
              resize: 'vertical',
              ...style,
            }}
            className={`input-textarea ${className}`}
            {...props}
          />
        )}
      </FormField>
    );
  }
);
TextArea.displayName = 'TextArea';

/* ==========================================================================
   Select Component
   ========================================================================== */
export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  helperText?: string;
  error?: string;
  options: SelectOption[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, helperText, error, required, options, className = '', style, ...props }, ref) => {
    return (
      <FormField label={label} helperText={helperText} error={error} required={required} id={props.id}>
        {(id, errorId, helperId) => (
          <select
            ref={ref}
            id={id}
            aria-invalid={!!error}
            aria-describedby={[errorId, helperId].filter(Boolean).join(' ') || undefined}
            style={{
              width: '100%',
              minHeight: 'var(--min-touch-target)',
              padding: '10px 14px',
              fontFamily: 'var(--font-sans)',
              fontSize: 'var(--text-body)',
              borderRadius: 'var(--radius-sm)',
              border: `1px solid ${error ? 'var(--danger)' : 'var(--border)'}`,
              backgroundColor: 'var(--surface)',
              color: 'var(--text-primary)',
              cursor: 'pointer',
              ...style,
            }}
            className={`input-select ${className}`}
            {...props}
          >
            {options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        )}
      </FormField>
    );
  }
);
Select.displayName = 'Select';

/* ==========================================================================
   Checkbox Component
   ========================================================================== */
export interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: React.ReactNode;
  helperText?: string;
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, helperText, id: customId, className = '', style, ...props }, ref) => {
    const autoId = useId();
    const id = customId || autoId;

    return (
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', marginBottom: '12px' }}>
        <input
          ref={ref}
          type="checkbox"
          id={id}
          style={{
            width: '20px',
            height: '20px',
            marginTop: '2px',
            accentColor: 'var(--brand)',
            cursor: 'pointer',
            ...style,
          }}
          className={`input-checkbox ${className}`}
          {...props}
        />
        <div>
          <label htmlFor={id} style={{ fontSize: 'var(--text-body-small)', fontWeight: 500, cursor: 'pointer', color: 'var(--text-primary)' }}>
            {label}
          </label>
          {helperText && (
            <p style={{ fontSize: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: '2px' }}>
              {helperText}
            </p>
          )}
        </div>
      </div>
    );
  }
);
Checkbox.displayName = 'Checkbox';

/* ==========================================================================
   Radio Component
   ========================================================================== */
export interface RadioProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: React.ReactNode;
  helperText?: string;
}

export const Radio = forwardRef<HTMLInputElement, RadioProps>(
  ({ label, helperText, id: customId, className = '', style, ...props }, ref) => {
    const autoId = useId();
    const id = customId || autoId;

    return (
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', marginBottom: '10px' }}>
        <input
          ref={ref}
          type="radio"
          id={id}
          style={{
            width: '20px',
            height: '20px',
            marginTop: '2px',
            accentColor: 'var(--brand)',
            cursor: 'pointer',
            ...style,
          }}
          className={`input-radio ${className}`}
          {...props}
        />
        <div>
          <label htmlFor={id} style={{ fontSize: 'var(--text-body-small)', fontWeight: 500, cursor: 'pointer', color: 'var(--text-primary)' }}>
            {label}
          </label>
          {helperText && (
            <p style={{ fontSize: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: '2px' }}>
              {helperText}
            </p>
          )}
        </div>
      </div>
    );
  }
);
Radio.displayName = 'Radio';

/* ==========================================================================
   Switch (Toggle) Component
   ========================================================================== */
export interface SwitchProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  id?: string;
  className?: string;
}

export const Switch: React.FC<SwitchProps> = ({
  label,
  checked,
  onChange,
  disabled = false,
  id: customId,
  className = '',
}) => {
  const autoId = useId();
  const id = customId || autoId;

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', minHeight: 'var(--min-touch-target)' }}>
      <label htmlFor={id} style={{ fontSize: 'var(--text-body-small)', fontWeight: 500, color: 'var(--text-primary)', cursor: disabled ? 'not-allowed' : 'pointer' }}>
        {label}
      </label>
      <button
        id={id}
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        type="button"
        style={{
          width: '46px',
          height: '26px',
          borderRadius: 'var(--radius-pill)',
          backgroundColor: checked ? 'var(--brand)' : 'var(--border-strong)',
          border: 'none',
          position: 'relative',
          cursor: disabled ? 'not-allowed' : 'pointer',
          opacity: disabled ? 0.6 : 1,
          transition: 'background-color var(--duration-fast)',
        }}
        className={`input-switch ${className}`}
      >
        <span
          style={{
            position: 'absolute',
            top: '3px',
            left: checked ? '23px' : '3px',
            width: '20px',
            height: '20px',
            borderRadius: '50%',
            backgroundColor: 'var(--color-white)',
            transition: 'left var(--duration-fast) var(--ease-standard)',
            boxShadow: 'var(--shadow-subtle)',
          }}
        />
      </button>
    </div>
  );
};

/* ==========================================================================
   SearchInput Component
   ========================================================================== */
export const SearchInput = forwardRef<HTMLInputElement, Omit<TextInputProps, 'leftAddon'>>(
  ({ placeholder = 'Search projects, designers, or categories...', ...props }, ref) => {
    return (
      <TextInput
        ref={ref}
        type="search"
        placeholder={placeholder}
        leftAddon={<Search size={16} aria-hidden="true" />}
        {...props}
      />
    );
  }
);
SearchInput.displayName = 'SearchInput';
