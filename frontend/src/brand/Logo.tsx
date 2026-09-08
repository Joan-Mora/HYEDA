export function Logo({ size = 36 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-label="HEA">
      <circle cx="32" cy="32" r="32" fill="#1B1F4C" />
      <text x="32" y="40" textAnchor="middle" fill="#F4F4F6" fontFamily="Oswald, sans-serif" fontSize="18" fontWeight="600">HEA</text>
    </svg>
  )
}
