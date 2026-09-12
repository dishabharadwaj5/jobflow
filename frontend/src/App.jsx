import { useEffect, useMemo, useState } from "react";
import "./App.css";

const API = "/api/jobs";
const SYSTEM_API = "/api/system";
const PRIORITY = { HIGH: 3, MEDIUM: 2, LOW: 1 };

const formatDuration = (ms) => {
  if (!ms) return "—";
  return `${(ms / 1000).toFixed(1)}s`;
};

const formatTime = (value) => value ? new Date(value).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";

function App() {
  const [jobs, setJobs] = useState([]);
  const [system, setSystem] = useState(null);
  const [name, setName] = useState("");
  const [priority, setPriority] = useState("MEDIUM");
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [cancelling, setCancelling] = useState(null);

  const loadData = async () => {
    try {
      const [jobsResponse, systemResponse] = await Promise.all([fetch(API), fetch(SYSTEM_API)]);
      if (!jobsResponse.ok || !systemResponse.ok) throw new Error("Backend unavailable");
      const [jobData, systemData] = await Promise.all([jobsResponse.json(), systemResponse.json()]);
      setJobs(Array.isArray(jobData) ? jobData : []);
      setSystem(systemData);
      setConnected(true);
      setError("");
    } catch {
      setConnected(false);
      setError("Backend unavailable. Start Spring Boot on port 8080.");
    }
  };

  useEffect(() => {
    loadData();
    const timer = setInterval(loadData, 1000);
    return () => clearInterval(timer);
  }, []);

  const createJob = async (event) => {
    event.preventDefault();
    if (!name.trim()) {
      setError("Enter a workload name first.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const response = await fetch(API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim(), priority }),
      });
      if (!response.ok) throw new Error((await response.text()) || "Could not create job");
      setName("");
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadDemo = async () => {
    setLoading(true);
    setError("");
    const demo = [
      ["Generate monthly report", "LOW"],
      ["Charge customer payment", "HIGH"],
      ["Send customer email", "MEDIUM"],
      ["Fraud screening", "HIGH"],
      ["Update search index", "LOW"],
      ["fail payment simulation", "HIGH"],
      ["Rebuild analytics cache", "MEDIUM"],
      ["Send webhook notification", "LOW"],
    ];
    try {
      for (const [jobName, jobPriority] of demo) {
        const response = await fetch(API, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name: jobName, priority: jobPriority }),
        });
        if (!response.ok) throw new Error("Could not load demo queue");
      }
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const cancelJob = async (id) => {
    setCancelling(id);
    try {
      const response = await fetch(`${API}/${id}/cancel`, { method: "POST" });
      if (!response.ok) throw new Error((await response.text()) || "Could not cancel job");
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setCancelling(null);
    }
  };

  const counts = useMemo(() => ({
    total: jobs.length,
    queued: jobs.filter((j) => j.status === "QUEUED").length,
    running: jobs.filter((j) => j.status === "RUNNING").length,
    retrying: jobs.filter((j) => j.status === "RETRYING").length,
    completed: jobs.filter((j) => j.status === "COMPLETED").length,
    failed: jobs.filter((j) => j.status === "FAILED").length,
    cancelled: jobs.filter((j) => j.status === "CANCELLED").length,
  }), [jobs]);

  const queue = useMemo(() => jobs
    .filter((j) => j.status === "QUEUED" || j.status === "RETRYING")
    .sort((a, b) => (PRIORITY[b.priority] - PRIORITY[a.priority]) || (a.createdAt - b.createdAt)), [jobs]);

  const filtered = useMemo(() => jobs.filter((job) => {
    const q = search.toLowerCase().trim();
    return (!q || job.name?.toLowerCase().includes(q) || job.id?.toLowerCase().includes(q))
      && (statusFilter === "ALL" || job.status === statusFilter)
      && (priorityFilter === "ALL" || job.priority === priorityFilter);
  }), [jobs, search, statusFilter, priorityFilter]);

  const avgRuntime = useMemo(() => {
    const completed = jobs.filter((j) => j.executionDurationMs > 0);
    return completed.length ? completed.reduce((sum, j) => sum + j.executionDurationMs, 0) / completed.length : 0;
  }, [jobs]);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <div className="logo">JF</div>
          <div><strong>JobFlow</strong><span>Concurrent background job orchestration</span></div>
        </div>
        <div className={`live ${connected ? "on" : "off"}`}><i />{connected ? "SYSTEM ONLINE" : "BACKEND OFFLINE"}</div>
      </header>

      <main>
        <section className="hero">
          <div>
            <p className="label">OPERATIONS CONSOLE</p>
            <h1>Watch workloads move<br /><span>through the pipeline.</span></h1>
            <p className="hero-copy">A priority queue feeds three concurrent workers. Jobs report progress, retry after failures, and can be cancelled while running.</p>
          </div>
          <div className="architecture">
            <div className="arch-title">LIVE PIPELINE</div>
            <div className="flow"><b>API</b><em>→</em><b>PRIORITY QUEUE</b><em>→</em><b>3 WORKERS</b></div>
            <div className="flow-sub">Thread-safe scheduling · automatic retry · in-memory state</div>
          </div>
        </section>

        <section className="stats">
          <Stat title="Queue depth" value={system?.queueDepth ?? counts.queued} detail="waiting workloads" />
          <Stat title="Active workers" value={`${system?.activeWorkers ?? counts.running}/${system?.workerCount ?? 3}`} detail="concurrent capacity" />
          <Stat title="Completed" value={counts.completed} detail="successful workloads" />
          <Stat title="Retry attempts" value={system?.retries ?? 0} detail="backoff events" />
          <Stat title="Avg runtime" value={formatDuration(avgRuntime)} detail="completed jobs" />
        </section>

        <section className="workspace">
          <div className="panel submit">
            <div className="panel-head"><div><p className="label">QUEUE WORK</p><h2>Submit a workload</h2></div><button className="secondary" onClick={loadDemo} disabled={loading}>Run demo scenario</button></div>
            <form onSubmit={createJob}>
              <label>WORKLOAD NAME<input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Charge customer payment" /></label>
              <label>PRIORITY<select value={priority} onChange={(e) => setPriority(e.target.value)}><option>HIGH</option><option>MEDIUM</option><option>LOW</option></select></label>
              <button className="primary" disabled={loading}>{loading ? "Submitting…" : "Enqueue job"}</button>
            </form>
            <p className="hint">Tip: include <code>fail</code> in the name to demonstrate the retry + exponential backoff path.</p>
            {error && <div className="error">{error}</div>}
          </div>
        </section>

        <section className="workers-section">
          <div className="section-row"><div><p className="label">EXECUTION POOL</p><h2>Worker activity</h2></div><span className="capacity">{system?.activeWorkers ?? 0} active · {system?.idleWorkers ?? 3} idle</span></div>
          <div className="worker-grid">
            {(system?.workers || [1,2,3].map((workerId) => ({ workerId, status: "IDLE", progress: 0 }))).map((worker) => {
              const active = worker.status === "RUNNING";
              const retrying = worker.status === "RETRYING";
              return <div className={`worker-card ${active ? "active" : ""}`} key={worker.workerId}>
                <div className="worker-top"><span>WORKER {String(worker.workerId).padStart(2, "0")}</span><b className={active ? "busy" : "idle"}><i />{active ? "RUNNING" : "IDLE"}</b></div>
                {active ? <>
                  <div className="worker-job"><strong>{worker.jobName}</strong><span>Job {worker.jobId?.slice(0, 8)}…</span></div>
                  <div className="progress"><span style={{ width: `${worker.progress || 0}%` }} /></div>
                  <div className="worker-foot"><span>{worker.progress || 0}% complete</span><span>Thread-safe worker</span></div>
                </> : <div className="worker-empty"><div className="worker-ring">{retrying ? "↻" : "—"}</div><span>{retrying ? "Waiting for retry" : "Ready for next job"}</span></div>}
              </div>;
            })}
          </div>
        </section>

        <section className="lower-grid">
          <div className="panel queue-panel"><div className="panel-head"><div><p className="label">SCHEDULER</p><h2>Next up</h2></div><span className="queue-count">{queue.length} waiting</span></div>
            {queue.length ? <div className="queue-list">{queue.slice(0, 7).map((job) => <div className="queue-row" key={job.id}><span className={`priority ${job.priority.toLowerCase()}`}>{job.priority[0]}</span><div><strong>{job.name}</strong><small>{job.status === "RETRYING" ? `retry ${job.retryCount}/${3}` : "awaiting worker"}</small></div><b>{job.status}</b></div>)}</div> : <div className="empty">No workloads waiting. Workers will take the next job automatically.</div>}
          </div>
          <div className="panel metrics-panel"><div className="panel-head"><div><p className="label">SYSTEM HEALTH</p><h2>Runtime signals</h2></div></div><Health label="Success rate" value={`${system?.successRate ?? 0}%`} /><Health label="Total workloads" value={counts.total} /><Health label="Failed" value={counts.failed} /><Health label="Cancelled" value={counts.cancelled} /><Health label="Retrying now" value={counts.retrying} /></div>
        </section>

        <section className="panel history"><div className="panel-head"><div><p className="label">JOB HISTORY</p><h2>Workload timeline</h2></div><span className="muted">Live · refreshes every second</span></div>
          <div className="toolbar"><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search workloads or IDs…" /><select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option>ALL</option><option>QUEUED</option><option>RUNNING</option><option>RETRYING</option><option>COMPLETED</option><option>FAILED</option><option>CANCELLED</option></select><select value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)}><option>ALL</option><option>HIGH</option><option>MEDIUM</option><option>LOW</option></select></div>
          <div className="table-wrap"><table><thead><tr><th>WORKLOAD</th><th>PRIORITY</th><th>STATUS</th><th>PROGRESS</th><th>RETRIES</th><th>RUNTIME</th><th>WORKER</th><th>ACTION</th></tr></thead><tbody>{filtered.map((job) => <tr key={job.id}><td><strong>{job.name}</strong><small>{job.id}</small></td><td><span className={`priority-text ${job.priority.toLowerCase()}`}>{job.priority}</span></td><td><span className={`status ${job.status.toLowerCase()}`}><i />{job.status}</span></td><td><div className="table-progress"><div><span style={{ width: `${job.progress || 0}%` }} /></div><small>{job.progress || 0}%</small></div></td><td>{job.retryCount || 0}</td><td>{formatDuration(job.executionDurationMs)}</td><td>{job.workerId ? `#${job.workerId}` : "—"}</td><td>{["QUEUED", "RUNNING", "RETRYING"].includes(job.status) ? <button className="cancel" disabled={cancelling === job.id} onClick={() => cancelJob(job.id)}>{cancelling === job.id ? "…" : "Cancel"}</button> : "—"}</td></tr>)}</tbody></table>{filtered.length === 0 && <div className="empty">No workloads match the current filters.</div>}</div>
        </section>

        <footer><strong>JobFlow</strong><span>Priority scheduling</span><span>3-worker concurrency</span><span>Retry backoff</span><span>Spring Boot + React</span></footer>
      </main>
    </div>
  );
}

function Stat({ title, value, detail }) { return <div className="stat"><span>{title}</span><strong>{value}</strong><small>{detail}</small></div>; }
function Health({ label, value }) { return <div className="health"><span>{label}</span><strong>{value}</strong></div>; }

export default App;
