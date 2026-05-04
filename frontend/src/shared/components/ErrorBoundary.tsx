// src/shared/components/ErrorBoundary.tsx
// Catches runtime errors and displays them instead of a blank screen

import React from 'react';

interface State { error: Error | null; }
interface Props { children: React.ReactNode; }

class ErrorBoundary extends React.Component<Props, State> {
 constructor(props: Props) {
   super(props);
   this.state = { error: null };
 }

 static getDerivedStateFromError(error: Error): State {
   return { error };
 }

 render() {
   if (this.state.error) {
     return (
       <div style={{
         padding:    '2rem',
         fontFamily: 'monospace',
         color:      '#dc2626',
         background: '#fef2f2',
         minHeight:  '100vh',
       }}>
         <strong>Runtime error:</strong>
         <pre style={{ marginTop: '1rem', fontSize: '0.8rem', whiteSpace: 'pre-wrap' }}>
           {this.state.error.message}{'\n'}{this.state.error.stack}
         </pre>
         <button
           onClick={() => this.setState({ error: null })}
           style={{
             marginTop:    '1rem',
             padding:      '6px 14px',
             cursor:       'pointer',
             borderRadius: 6,
             border:       '1px solid #dc2626',
             background:   'none',
             color:        '#dc2626',
             fontFamily:   'inherit',
           }}
         >
           Dismiss
         </button>
       </div>
     );
   }
   return this.props.children;
 }
}

export default ErrorBoundary;
 