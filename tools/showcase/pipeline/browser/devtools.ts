/** A connection to a browser over the Chrome DevTools Protocol, with Node's own WebSocket. */

// Longer than any page of the sync server takes to load.
const COMMAND_TIMEOUT_MS = 60_000;

interface Message {
  id?: number;
  method?: string;
  params?: Record<string, unknown>;
  result?: Record<string, unknown>;
  error?: { message: string };
  sessionId?: string;
}

export class DevTools {
  private readonly socket: WebSocket;
  private nextId = 1;
  private readonly pending = new Map<
    number,
    { resolve: (result: Record<string, unknown>) => void; reject: (error: Error) => void }
  >();
  private readonly listeners = new Set<(message: Message) => void>();

  private constructor(socket: WebSocket) {
    this.socket = socket;
    socket.addEventListener("message", (event) => {
      const message = JSON.parse(String(event.data)) as Message;
      if (message.id !== undefined) {
        const call = this.pending.get(message.id);
        this.pending.delete(message.id);
        if (message.error) call?.reject(new Error(message.error.message));
        else call?.resolve(message.result ?? {});
      } else {
        for (const listener of this.listeners) listener(message);
      }
    });
  }

  static async connect(url: string): Promise<DevTools> {
    const socket = new WebSocket(url);
    await new Promise<void>((resolve, reject) => {
      socket.addEventListener("open", () => resolve(), { once: true });
      socket.addEventListener("error", () => reject(new Error(`Can't reach the browser at ${url}`)), { once: true });
    });
    return new DevTools(socket);
  }

  /** Sends [method] to the browser, or to the page [sessionId] attaches to, and returns its result. */
  send(method: string, params: Record<string, unknown> = {}, sessionId?: string): Promise<Record<string, unknown>> {
    const id = this.nextId++;
    const answered = new Promise<Record<string, unknown>>((resolve, reject) => this.pending.set(id, { resolve, reject }));
    this.socket.send(JSON.stringify({ id, method, params, ...(sessionId === undefined ? {} : { sessionId }) }));
    return withTimeout(answered, `${method} got no answer`);
  }

  /** Waits for the next [method] event of [sessionId]'s page. Ask for it before the command that leads to it. */
  event(method: string, sessionId: string): Promise<void> {
    let listener: (message: Message) => void = () => {};
    const fired = new Promise<void>((resolve) => {
      listener = (message) => {
        if (message.method === method && message.sessionId === sessionId) resolve();
      };
      this.listeners.add(listener);
    });
    return withTimeout(fired, `${method} never came`).finally(() => this.listeners.delete(listener));
  }

  close(): void {
    this.socket.close();
  }
}

function withTimeout<T>(promise: Promise<T>, message: string): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(message)), COMMAND_TIMEOUT_MS);
    promise.then(
      (value) => {
        clearTimeout(timer);
        resolve(value);
      },
      (error: unknown) => {
        clearTimeout(timer);
        reject(error);
      },
    );
  });
}
