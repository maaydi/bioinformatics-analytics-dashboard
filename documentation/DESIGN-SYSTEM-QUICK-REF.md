# Design System Quick Reference for Genes Component

## Quick Links

- **Full Token Reference**: `frontend/src/styles/_design-system.scss`
- **Implementation Guide**: `documentation/genes-design-system.md`
- **Completion Report**: `documentation/implementation/GENES-STYLE-COHERENCE/COMPLETION_REPORT.md`

---

## Typography Mixins (Quick Use)

```scss
@include ds.text-headline-sm; // 18px, 600 weight, section titles
@include ds.text-title-md; // 16px, 600 weight, subheadings
@include ds.text-title-sm; // 14px, 600 weight, filter headers
@include ds.text-body-md; // 14px, 400 weight, body content
@include ds.text-body-sm; // 12px, 400 weight, table cells
@include ds.text-label-md; // 12px, 500 weight, button labels
@include ds.text-label-sm; // 11px, 500 weight, badges
@include ds.text-caption; // 10px, 400 weight, footnotes
@include ds.text-mono; // Monospace (accessions, codes)
```

## Color Tokens (Variable Form)

```scss
// Semantic
$color-surface: #ffffff;
$color-surface-subtle: #f9fafb;
$color-surface-hover: #f3f4f6;
$color-on-surface: #1a1a1a;
$color-on-surface-variant: #6b7280;
$color-border-light: #e5e7eb;

// Material Design 3
var
(
--mat-sys-primary

)
var
(
--mat-sys-error

)
var
(
--mat-sys-primary-container

)
var
(
--mat-sys-on-primary-container

)

// Special (Preserved)
$color-evidence-high: #2e7d32;
$color-evidence-medium: #e65100;
$color-evidence-low: #c62828;
$color-success-bg: #e8f5e9;
$color-accent-bg: #eef2ff;
```

## Spacing Tokens

```scss
ds.$spacing-xs // 4px
ds.$spacing-sm // 8px
ds.$spacing-md // 12px
ds.$spacing-lg // 16px
ds.$spacing-xl // 24px
ds.$spacing-2xl

// 32px

@mixin gap-xs {
}

// gap: 4px
@mixin gap-sm {
}

// gap: 8px
etc...
```

## Responsive Queries

```scss
@include ds.respond-to('lg') {
  // HD: 1366×768 and up
}

@include ds.respond-to('xl') {
  // FHD: 1920×1080 and up
}

@include ds.respond-up-to('md') {
  // Tablet and down: max 1024px
}
```

## Utility Mixins

```scss
@include ds.flex-center; // Centered flex container
@include ds.flex-between; // Space-between layout
@include ds.truncate; // Text overflow ellipsis
@include ds.line-clamp(2); // Max 2 lines
@include ds.focus-ring(); // Accessible focus indicator
@include ds.shadow-sm; // Material shadow
@include ds.badge-base; // Badge styling foundation
@include ds.chip-base; // Chip styling foundation
```

## Common Component Patterns

### Form Input

```scss
@include ds.text-body-sm;
padding: ds.$spacing-sm ds.$spacing-md

;
border: ds.$border-width-thin solid ds.$color-border-light

;
border-radius: ds.$border-radius-md

;
color: ds.$color-on-surface

;
transition: border-color

0.2
s ease

;

&:focus {
  border-color: var(--mat-sys-primary);
  @include ds.shadow-sm;
}
```

### Button

```scss
@include ds.text-body-sm;
padding: ds.$spacing-sm

(
ds.$spacing-md *

1.5
)
;
border-radius: ds.$border-radius-md

;
font-weight: ds.$font-weight-medium

;
cursor: pointer

;
transition: all

0.2
s ease

;

@include ds.focus-visible;
```

### Badge

```scss
@include ds.badge-base;
background-color:

var
(
--mat-sys-primary-container

)
;
color:

var
(
--mat-sys-on-primary-container

)
;
padding: ds.$spacing-xs ds.$spacing-md

;
border-radius: ds.$border-radius-full

;
```

### Table Cell

```scss
@include ds.text-body-sm;
padding: ds.$spacing-md ds.$spacing-lg

;
height:

40
px

;
display: flex

;
align-items: center

;
border-bottom: ds.$border-width-thin solid ds.$color-border-light

;
background-color: ds.$color-surface

;

&:hover {
  background-color: ds.$color-surface-hover;
}
```

## Preserved Colors - Critical

**DO NOT CHANGE**:

- Evidence Level: Green/Orange/Red badges
- Reviewed Status: #e8f5e9 / #fff3e0 backgrounds
- Keywords: #eef2ff background, #374151 text

These are explicitly preserved per design specification.

## Import Statement

```scss
@use "design-system" as ds;
```

All SCSS files in `features/genes/` should include this at the top.

## Example: New Component Styling

```scss
@use "design-system" as ds;

.my-component-title {
  @include ds.text-title-md;
  color: ds.$color-on-surface;
  margin-bottom: ds.$spacing-md;
}

.my-component-input {
  @include ds.text-body-sm;
  padding: ds.$spacing-sm ds.$spacing-md;
  border: ds.$border-width-thin solid ds.$color-border-light;
  border-radius: ds.$border-radius-md;
  color: ds.$color-on-surface;

  @include ds.focus-visible;
}

@include ds.respond-to('xl') {
  .my-component-title {
    font-size: 18px; // Optional: enhance for FHD
  }
}
```

## HD/FHD Testing

### HD (1366×768) Screenshot Points

- Sidebar width: 280–340px ✓
- Main content: ~1000px ✓
- Table min-height: 460px ✓
- All text readable at 12px ✓

### FHD (1920×1080) Screenshot Points

- Sidebar width: 300–380px ✓
- Main content: ~1500px ✓
- Table min-height: 500px ✓
- Enhanced padding visible ✓

## Troubleshooting

### "Undefined variable ds.$color-surface"

- Check that `@use "design-system" as ds;` is at the top of the file
- Ensure file imports from correct path (check `_design-system.scss` location)

### "Typography looks inconsistent"

- Use `@include ds.text-*` mixins instead of hand-written font rules
- Verify all components import the design system

### "Colors don't match"

- Use exact token values (e.g., `ds.$color-on-surface` not hardcoded `#1a1a1a`)
- For Material tokens, use `var(--mat-sys-primary)`

### "Responsive breakpoint not triggering"

- Use `@include ds.respond-to('xl')` for ≥1920px, not custom media queries
- Check that breakpoint name matches list (lg, xl, 2xl, etc.)

---

**Last Updated**: May 25, 2026  
**Design System Version**: 1.0  
**Target Displays**: HD (1366×768), FHD (1920×1080)

