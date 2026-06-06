// Mensaje de estado (success / error / info).
export default function Alert({ type = 'info', children }) {
  const icon =
    type === 'success'
      ? 'bi-check-circle-fill'
      : type === 'error'
        ? 'bi-exclamation-triangle-fill'
        : 'bi-info-circle-fill'
  return (
    <div className={'alert alert-' + type}>
      <i className={'bi ' + icon} />
      <span>{children}</span>
    </div>
  )
}
