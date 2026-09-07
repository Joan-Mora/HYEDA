import { useEffect, useRef, useState } from 'react'
import { motion, useSpring, useTransform } from 'framer-motion'

export function AnimatedNumber({
  value,
  format = (n) => String(Math.round(n)),
  className = '',
}: {
  value: number
  format?: (n: number) => string
  className?: string
}) {
  const spring = useSpring(0, { stiffness: 70, damping: 22, mass: 0.6 })
  const display = useTransform(spring, (v) => format(v))
  const [text, setText] = useState(format(0))
  const started = useRef(false)

  useEffect(() => {
    if (!started.current) {
      started.current = true
      spring.set(0)
    }
    spring.set(value)
  }, [spring, value])

  useEffect(() => {
    const unsub = display.on('change', (v) => setText(v))
    return () => unsub()
  }, [display])

  return (
    <motion.span className={className} aria-label={format(value)}>
      {text}
    </motion.span>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`skeleton ${className}`} aria-hidden />
}

export function SkeletonRows({ rows = 5 }: { rows?: number }) {
  return (
    <div className="p-4 space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex gap-3 items-center">
          <Skeleton className="h-10 w-10 rounded-lg shrink-0" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-3 w-[70%]" />
            <Skeleton className="h-2.5 w-[45%]" />
          </div>
        </div>
      ))}
    </div>
  )
}

export function StatusPulse({
  status,
  label,
}: {
  status: 'ok' | 'en_calle' | 'vencido' | 'muted'
  label: string
}) {
  return (
    <span className={`pulse-badge pulse-${status}`}>
      <span className="pulse-dot" aria-hidden />
      {label}
    </span>
  )
}
