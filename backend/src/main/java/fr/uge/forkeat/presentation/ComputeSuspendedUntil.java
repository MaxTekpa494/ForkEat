package fr.uge.forkeat.presentation;

import fr.uge.forkeat.service.model.user.UserModerationActionType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ComputeSuspendedUntil {
  public static Instant computeSuspendedUntil(UserModerationActionType action, Integer suspensionDays, Integer suspensionHours) {
    if (action != UserModerationActionType.SUSPENDED) {
      return null;
    }
    int days = suspensionDays == null ? 0 : suspensionDays;
    int hours = suspensionHours == null ? 0 : suspensionHours;
    if (days < 0 || hours < 0 || hours > 23) {
      throw new IllegalArgumentException("Suspension duration is invalid");
    }
    if (days == 0 && hours == 0) {
      throw new IllegalArgumentException("Suspension duration must be greater than zero");
    }
    return Instant.now().plus(days, ChronoUnit.DAYS).plus(hours, ChronoUnit.HOURS);
  }
}
