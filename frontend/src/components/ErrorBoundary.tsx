import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { error: Error | null }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('UI crash:', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="p-8 max-w-lg mx-auto space-y-3">
          <h1 className="font-display text-2xl text-[var(--navy)] uppercase">Error en la pantalla</h1>
          <p className="text-sm text-[var(--text-muted)]">{this.state.error.message}</p>
          <button
            type="button"
            className="btn btn-navy"
            onClick={() => {
              this.setState({ error: null })
              window.location.assign('/')
            }}
          >
            Volver al inicio
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
