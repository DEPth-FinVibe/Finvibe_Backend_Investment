package depth.finvibe.investment.boot.security.model;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Requester {
    private UUID uuid;
    private UserRole role;
}
