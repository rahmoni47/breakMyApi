import Card from "./Card";
import Button from "./Button";

export default function ConfirmDialog({
  open,
  title,
  description,
  confirmText = "Confirm",
  cancelText = "Cancel",
  onConfirm,
  onCancel,
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/35 px-4">
      <Card className="w-full max-w-md p-6">
        <h3 className="text-lg font-bold text-text-main">{title}</h3>
        <p className="mt-2 text-sm text-text-secondary">{description}</p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={onCancel}>
            {cancelText}
          </Button>
          <Button variant="danger" onClick={onConfirm}>
            {confirmText}
          </Button>
        </div>
      </Card>
    </div>
  );
}
