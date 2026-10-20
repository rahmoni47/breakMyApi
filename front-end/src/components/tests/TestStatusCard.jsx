import Card from "../common/Card";
import { Loader2 } from "lucide-react";

export default function TestStatusCard({ run }) {
  return (
    <Card className="p-5">
      <div className="flex items-start gap-4">
        <div className="relative flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-100 text-primary">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full bg-warning animate-pulseDot" />
        </div>
        <div>
          <p className="text-2xl font-bold text-text-main">
            Test Run in Progress
          </p>
          <p className="mt-1 text-sm text-text-secondary">
            {run?.progress?.phase ||
              "Our test is currently in progress. We're waiting for API response."}
          </p>
        </div>
      </div>
    </Card>
  );
}
