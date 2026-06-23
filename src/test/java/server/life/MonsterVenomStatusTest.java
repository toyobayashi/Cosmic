package server.life;

import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MonsterVenomStatusTest {

    @Test
    void venomDamageUsesSingleClientDamageStatus() {
        MonsterStatusEffect status = new MonsterStatusEffect(
                Collections.singletonMap(MonsterStatus.POISON, 1),
                null,
                null,
                false);

        Monster.setVenomDamageStatus(status, Short.MAX_VALUE);

        assertEquals(1, status.getStati().size());
        assertEquals(Short.MAX_VALUE, status.getStati().get(MonsterStatus.VENOMOUS_WEAPON));
        assertFalse(status.getStati().containsKey(MonsterStatus.POISON));
    }
}
