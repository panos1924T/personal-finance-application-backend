package gr.cf9.pants.expense_tracker.dto;

public record AuthenticationResponseDTO(
        String token,
        String displayName
) {
}
