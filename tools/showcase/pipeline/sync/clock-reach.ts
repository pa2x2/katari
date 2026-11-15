/**
 * How far the clocks of a group's devices have run. Each scene sets its device's clock back to where it starts, but
 * sync stamps a device's change after every change the device has made or taken in, so stamps carry on from the
 * latest moment any earlier scene reached. A change only sorts after one another device made without it having seen
 * that one, as the desktop's reading on comes after the page the phone's reader opened at, when it is made past that
 * moment.
 */
export class ClockReach {
  private reached = 0;

  /** The latest moment a device's clock has read at the end of a scene. */
  get latest(): number {
    return this.reached;
  }

  /**
   * Runs [scene] on a device whose clock it starts at [startsAt], and keeps where that clock got to. The time is taken
   * from before the scene sets the clock, which puts the moment a little late and never early.
   */
  async track<T>(startsAt: number, scene: () => Promise<T>): Promise<T> {
    const start = Date.now();
    try {
      return await scene();
    } finally {
      this.reached = Math.max(this.reached, startsAt + (Date.now() - start));
    }
  }
}
