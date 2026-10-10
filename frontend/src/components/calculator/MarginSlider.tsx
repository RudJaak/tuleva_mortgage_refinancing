import type { CSSProperties } from 'react'

const min = 0.5
const max = 3
const step = 0.05

function position(value: number) {
  return Math.min(Math.max((value - min) / (max - min), 0), 1)
}

function thumbCentre(value: number): CSSProperties {
  const fraction = position(value)
  return { left: `calc(${fraction * 100}% + ${(0.5 - fraction) * 1.5}rem)` }
}

export function MarginSlider({
  id,
  value,
  marker,
  markerLabel,
  format,
  onChange,
}: {
  id: string
  value: number
  marker: number
  markerLabel: string
  format: (value: number) => string
  onChange: (value: number) => void
}) {
  return (
    <div className="margin-slider">
      <span className="margin-slider-value" style={thumbCentre(value)} aria-hidden="true">
        {format(value)}
      </span>
      <input
        id={id}
        type="range"
        className="form-range"
        min={min}
        max={max}
        step={step}
        value={value}
        aria-valuetext={format(value)}
        onChange={(event) => onChange(Number(event.target.value))}
      />
      <span className="margin-slider-marker" style={thumbCentre(marker)} aria-hidden="true" />
      <span className="margin-slider-marker-label small text-body-secondary">{markerLabel}</span>
    </div>
  )
}
