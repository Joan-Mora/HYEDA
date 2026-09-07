type LogoProps = {
  size?: number
  variant?: 'navy' | 'light' | 'orange'
  className?: string
}

const palettes = {
  navy: { ring: '#1B1F4C', fill: '#1B1F4C', mark: '#F4F4F6' },
  light: { ring: '#F4F4F6', fill: 'transparent', mark: '#F4F4F6' },
  orange: { ring: '#E65100', fill: '#E65100', mark: '#FFFFFF' },
}

/**
 * HEA monogram restyling:
 * - Uniform stroke weight on H scaffold + circle
 * - More inner breathing room vs original
 * - Geometric alignment on a 64-unit grid
 */
export default function Logo({ size = 40, variant = 'navy', className }: LogoProps) {
  const c = palettes[variant]
  const hollow = variant === 'light'

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 64 64"
      width={size}
      height={size}
      className={className}
      role="img"
      aria-label="HEA — Herramientas y Equipos del Ariari"
    >
      {hollow ? (
        <circle cx="32" cy="32" r="29" fill="none" stroke={c.ring} strokeWidth="3.2" />
      ) : (
        <>
          <circle cx="32" cy="32" r="30" fill={c.fill} />
          <circle cx="32" cy="32" r="26.2" fill="none" stroke={c.mark} strokeWidth="1.8" opacity="0.35" />
        </>
      )}
      <g fill={c.mark}>
        {/* H pillars — equal weight */}
        <rect x="19.5" y="15.5" width="5.2" height="33" rx="0.5" />
        <rect x="39.3" y="15.5" width="5.2" height="33" rx="0.5" />
        {/* H crossbar */}
        <rect x="19.5" y="30.4" width="25" height="3.2" rx="0.4" />
        {/* E — upper bay, clearer counters */}
        <rect x="26.8" y="17.8" width="10.4" height="2.2" rx="0.2" />
        <rect x="26.8" y="17.8" width="2.2" height="10" rx="0.2" />
        <rect x="26.8" y="21.6" width="7.8" height="2.1" rx="0.2" />
        <rect x="26.8" y="25.6" width="10.4" height="2.2" rx="0.2" />
        {/* A — lower bay */}
        <path d="M32 34.8 L38.2 46.5 H35.2 L33.85 42.9 H30.15 L28.8 46.5 H25.8 Z M30.7 40.5 H33.3 L32 37.2 Z" />
      </g>
    </svg>
  )
}

export function BrandLockup({
  inverted = false,
  compact = false,
}: {
  inverted?: boolean
  compact?: boolean
}) {
  return (
    <div className="flex items-center gap-3 min-w-0">
      <Logo size={compact ? 36 : 44} variant={inverted ? 'light' : 'navy'} />
      <div className="min-w-0 leading-tight">
        <div
          className="font-display text-[1.35rem] tracking-[0.06em] uppercase"
          style={{ color: inverted ? '#F4F4F6' : 'var(--navy)' }}
        >
          HYEDA
        </div>
        {!compact && (
          <div
            className="text-[0.68rem] uppercase tracking-[0.08em] truncate"
            style={{ color: inverted ? 'rgba(244,244,246,0.72)' : 'var(--steel-muted)' }}
          >
            Herramientas y Equipos del Ariari
          </div>
        )}
      </div>
    </div>
  )
}
