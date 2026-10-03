/**
 * Class name concatenation — clsx benzeri minimal yardımcı.
 * Falsy değerleri atlar.
 */
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}
