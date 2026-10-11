/**
 * Turns a profile's stints into the activity Katari records while reading and watching: sessions, completions, chapter
 * history, read state, and playback positions. Days, times, and durations vary like a real person's would, but come
 * from a fixed seed, so every run writes the same history.
 */
import type {
  BackupActivityCompletion,
  BackupActivitySession,
  BackupHistory,
  BackupPlaybackState,
} from "./backup-messages.ts";
import { filmDurations, type ProfilePlan, type Stint } from "./library-plan.ts";
import { SeededRandom } from "./seeded-random.ts";
import { SHOWCASE_NOW, SHOWCASE_TIME_ZONE, STATISTICS_EPOCH } from "./showcase-clock.ts";

const MINUTE = 60_000;
const DAY = 86_400_000;

export interface WorkHistory {
  sessions: BackupActivitySession[];
  completions: BackupActivityCompletion[];
  history: BackupHistory[];
  readChapters: Set<number>;
  /** Page the reader stopped on, by chapter number, for chapters left unfinished. */
  lastPageRead: Map<number, number>;
  /** Where an unfinished book was left, as a fraction of the whole book. */
  bookProgress?: { chapter: number; progression: number; updatedAt: number };
  playbackStates: BackupPlaybackState[];
  firstActivity: number;
}

interface PlannedSession {
  stint: Stint;
  chapter: number;
  startedAt: number;
  duration: number;
}

export function readingHistory(
  plan: ProfilePlan,
  chapterNumbers: (work: string) => number[],
  seed: number,
): Map<string, WorkHistory> {
  const random = new SeededRandom(seed);
  const planned = plannedSessions(plan, chapterNumbers, random);
  const histories = new Map<string, WorkHistory>();
  for (const stint of plan.stints) {
    const history = histories.get(stint.work) ?? emptyHistory();
    histories.set(stint.work, history);
    const sessions = planned.filter((session) => session.stint === stint);
    const chapters = stintChapters(stint, chapterNumbers);
    for (const [index, chapter] of chapters.entries()) {
      const chapterSessions = sessions.filter((session) => session.chapter === chapter);
      if (chapterSessions.length === 0) continue;
      const recorded = chapterSessions.map((session) => record(session, chapterUrl(stint.work, chapter), random));
      history.sessions.push(...recorded);
      history.firstActivity = Math.min(history.firstActivity, recorded[0]!.startedAt);
      const last = recorded.at(-1)!;
      history.history.push({
        url: chapterUrl(stint.work, chapter),
        lastRead: last.endedAt,
        readDuration: recorded.reduce((total, session) => total + session.duration, 0),
      });
      const finished = index < chapters.length - 1 || stint.finishes;
      if (finished) {
        history.readChapters.add(chapter);
        history.completions.push({
          eventId: random.uuid(),
          chapterUrl: chapterUrl(stint.work, chapter),
          sessionId: last.sessionId,
          occurredAt: last.endedAt,
          localDate: isoDate(last.endedAt),
          timeZoneId: SHOWCASE_TIME_ZONE,
        });
      } else if (stint.stoppedAtPage !== undefined) {
        history.lastPageRead.set(chapter, stint.stoppedAtPage);
      } else if (stint.stoppedAtProgress !== undefined) {
        history.bookProgress = { chapter, progression: stint.stoppedAtProgress, updatedAt: last.endedAt };
      }
      const filmDuration = filmDurations[stint.work];
      if (filmDuration !== undefined) {
        history.playbackStates.push({
          url: chapterUrl(stint.work, chapter),
          positionMs: finished ? filmDuration : last.duration,
          durationMs: filmDuration,
          completed: finished,
          lastWatchedAt: last.endedAt,
        });
      }
    }
  }
  return histories;
}

export function chapterUrl(work: string, chapter: number): string {
  return `/${work}/${chapter}`;
}

function plannedSessions(plan: ProfilePlan, chapterNumbers: (work: string) => number[], random: SeededRandom) {
  const sessions: PlannedSession[] = [];
  const today = startOfDay(SHOWCASE_NOW);
  const streakStart = today - plan.goalStreakDays * DAY;
  const paced = pacedChapters(plan, chapterNumbers);
  for (let day = STATISTICS_EPOCH; day <= today; day += DAY) {
    const active = plan.stints.filter(
      (stint) =>
        stint.minutesPerChapter === undefined &&
        Date.parse(stint.from) <= day &&
        day <= Date.parse(stint.to) &&
        !onBreak(plan, day),
    );
    const dueToday = paced.filter((chapter) => chapter.day === day);
    if (active.length === 0 && dueToday.length === 0) continue;
    const isToday = day === today;
    const inStreak = day >= streakStart && day < today;
    const weekend = [0, 6].includes(new Date(day).getUTCDay());
    // A one-day stint, like watching a film, happens on its day whatever the odds.
    const planned = active.some((stint) => stint.from === stint.to);
    const chosen = isToday || inStreak || planned || random.next() <= (weekend ? 0.8 : 0.6);
    const sittings: Omit<PlannedSession, "startedAt">[] = dueToday.map(({ stint, chapter }) => ({
      stint,
      chapter,
      duration: random.between(stint.minutesPerChapter! - 3, stint.minutesPerChapter! + 3) * MINUTE,
    }));
    const pacedMinutes = sittings.reduce((total, sitting) => total + sitting.duration / MINUTE, 0);
    const minutes = !chosen
      ? 0
      : isToday
        ? Math.max(0, plan.minutesToday - pacedMinutes)
        : inStreak
          ? random.between(plan.dailyGoalMinutes + 2, plan.dailyGoalMinutes + 40)
          : random.between(12, 75);
    if (minutes > 0) {
      for (const stint of active) {
        const film = filmDurations[stint.work];
        const share = Math.max(5, Math.round(minutes / active.length));
        const durations =
          film !== undefined ? [film] : share > 45 ? [share - 20, 20].map((m) => m * MINUTE) : [share * MINUTE];
        const chapter = chapterOn(stint, day, chapterNumbers);
        sittings.push(...durations.map((duration) => ({ stint, chapter, duration })));
      }
    }
    if (sittings.length === 0) continue;
    const gaps = sittings.map(() => (isToday ? 2 : random.between(5, 40)) * MINUTE);
    const span = sittings.reduce((total, sitting, index) => total + sitting.duration + gaps[index]!, 0);
    // Today's reading must have ended before the screenshots are taken.
    let cursor = isToday ? SHOWCASE_NOW - span - 25 * MINUTE : day + startMinute(weekend, random) * MINUTE;
    for (const [index, sitting] of sittings.entries()) {
      sessions.push({ ...sitting, startedAt: cursor });
      cursor += sitting.duration + gaps[index]!;
    }
  }
  return sessions;
}

/** The day each chapter of a paced stint is read, spread evenly from its first day and moved past breaks. */
function pacedChapters(plan: ProfilePlan, chapterNumbers: (work: string) => number[]) {
  return plan.stints.flatMap((stint) => {
    if (stint.minutesPerChapter === undefined) return [];
    const chapters = stintChapters(stint, chapterNumbers);
    const from = Date.parse(stint.from);
    const days = (Date.parse(stint.to) - from) / DAY + 1;
    return chapters.map((chapter, index) => {
      let day = from + Math.floor((index * days) / chapters.length) * DAY;
      while (onBreak(plan, day)) day += DAY;
      return { stint, chapter, day };
    });
  });
}

function startMinute(weekend: boolean, random: SeededRandom): number {
  if (weekend) return random.between(10 * 60, 20 * 60);
  // Most weekday reading happens in the evening, some on the morning commute.
  return random.next() < 0.3 ? random.between(7 * 60 + 30, 8 * 60 + 15) : random.between(19 * 60, 22 * 60);
}

function chapterOn(stint: Stint, day: number, chapterNumbers: (work: string) => number[]): number {
  const chapters = stintChapters(stint, chapterNumbers);
  const from = Date.parse(stint.from);
  const progress = (day - from) / (Date.parse(stint.to) - from + DAY);
  return chapters[Math.min(chapters.length - 1, Math.floor(progress * chapters.length))]!;
}

function stintChapters(stint: Stint, chapterNumbers: (work: string) => number[]): number[] {
  const all = chapterNumbers(stint.work);
  return stint.chapters ? all.filter((n) => n >= stint.chapters!.first && n <= stint.chapters!.last) : all;
}

function record(session: PlannedSession, url: string, random: SeededRandom): BackupActivitySession {
  const endedAt = session.startedAt + session.duration;
  return {
    sessionId: random.uuid(),
    startedAt: session.startedAt,
    endedAt,
    duration: session.duration,
    segments: [
      {
        chapterUrl: url,
        localDate: isoDate(session.startedAt),
        timeZoneId: SHOWCASE_TIME_ZONE,
        startedAt: session.startedAt,
        endedAt,
        duration: session.duration,
      },
    ],
  };
}

function onBreak(plan: ProfilePlan, day: number): boolean {
  return plan.breaks.some((pause) => Date.parse(pause.from) <= day && day <= Date.parse(pause.to));
}

function emptyHistory(): WorkHistory {
  return {
    sessions: [],
    completions: [],
    history: [],
    readChapters: new Set(),
    lastPageRead: new Map(),
    playbackStates: [],
    firstActivity: Number.POSITIVE_INFINITY,
  };
}

function startOfDay(time: number): number {
  return time - (time % DAY);
}

function isoDate(time: number): string {
  return new Date(time).toISOString().slice(0, 10);
}
