"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from "@mui/material";
import { api } from "@/lib/api";
import { UserSession } from "@/types";
import { PageHeading, Notice, LoadingState, EmptyState } from "./ShopUI";
import Icon from "./Icon";
export default function SessionsView({ admin = false }: { admin?: boolean }) {
  const [sessions, setSessions] = useState<UserSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [target, setTarget] = useState<UserSession | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    (admin ? api.getAdminSessions() : api.getMySessions())
      .then(setSessions)
      .catch(() =>
        setError("Sessions could not be loaded. Refresh to try again."),
      )
      .finally(() => setLoading(false));
  }, [admin]);
  async function revoke() {
    if (!target || busy) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await (admin
        ? api.adminKickSession(target.id)
        : api.kickSession(target.id));
      setSessions(await (admin ? api.getAdminSessions() : api.getMySessions()));
      setMessage("This device has been signed out.");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Unable to sign out this device.",
      );
    } finally {
      setBusy(false);
      setTarget(null);
    }
  }
  const date = (time: number) =>
    new Date(time).toLocaleString("en-HK", {
      dateStyle: "medium",
      timeStyle: "short",
    });
  return (
    <>
      <PageHeading
        eyebrow={
          admin ? "KEEP YOUR STORE SECURE" : "YOUR ACCOUNT, SECURELY YOURS"
        }
        title={admin ? "Active sessions" : "Account & devices"}
        description={
          admin
            ? "Review active logins across your store and sign out a session when needed."
            : "Manage your password and the devices signed in to your account."
        }
      />
      {error && <Notice error>{error}</Notice>}
      {message && <Notice>{message}</Notice>}
      {!admin && (
        <section className="panel security-password">
          <div>
            <h2>A strong password, a safer account.</h2>
            <p className="muted">
              Update your password and sign in again with your new details.
            </p>
          </div>
          <Link className="button" href="/change-password">
            <Icon name="lock" size={16} />
            Change password
          </Link>
        </section>
      )}
      {loading ? (
        <LoadingState label="Checking active devices…" />
      ) : (
        <section className="panel">
          <h2>
            {admin ? "All active logins" : "Your active devices"}{" "}
            <span className="muted">({sessions.length})</span>
          </h2>
          {!sessions.length ? (
            <EmptyState
              icon="device"
              title="No active devices to show."
              description="Your active account sessions will appear here."
            />
          ) : admin ? (
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>SESSION</th>
                    <th>CUSTOMER</th>
                    <th>IP ADDRESS</th>
                    <th>SIGNED IN</th>
                    <th>ACTION</th>
                  </tr>
                </thead>
                <tbody>
                  {sessions.map((s) => (
                    <tr key={s.id}>
                      <td>#{s.id}</td>
                      <td>#{s.userid}</td>
                      <td>{s.ipAddress}</td>
                      <td>{date(s.loginTime)}</td>
                      <td>
                        {s.current ? (
                          <span className="status-pill paid">This device</span>
                        ) : (
                          <button
                            className="button"
                            disabled={busy}
                            onClick={() => setTarget(s)}
                          >
                            Sign out session
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            sessions.map((s) => (
              <div className="device-row" key={s.id}>
                <Icon name="device" size={24} />
                <div>
                  <h3>
                    {s.current ? "This device" : "Another signed-in device"}
                  </h3>
                  <p>
                    {s.ipAddress} · Signed in {date(s.loginTime)}
                  </p>
                </div>
                {s.current ? (
                  <span className="status-pill paid">Current device</span>
                ) : (
                  <button
                    className="button"
                    disabled={busy}
                    onClick={() => setTarget(s)}
                  >
                    Sign out
                  </button>
                )}
              </div>
            ))
          )}
        </section>
      )}
      <Dialog open={!!target} onClose={() => !busy && setTarget(null)}>
        <DialogTitle>Sign out this device?</DialogTitle>
        <DialogContent>
          This session will end immediately. The user can sign in again with
          their account details.
        </DialogContent>
        <DialogActions>
          <Button disabled={busy} onClick={() => setTarget(null)}>
            Keep signed in
          </Button>
          <Button disabled={busy} onClick={() => void revoke()}>
            Sign out device
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
