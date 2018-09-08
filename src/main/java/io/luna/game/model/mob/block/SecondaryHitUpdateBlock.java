package io.luna.game.model.mob.block;

import io.luna.game.model.mob.Hit;
import io.luna.game.model.mob.Npc;
import io.luna.game.model.mob.Player;
import io.luna.game.model.mob.block.UpdateFlagSet.UpdateFlag;
import io.luna.net.codec.ByteMessage;
import io.luna.net.codec.ByteTransform;

/**
 * An {@link UpdateBlock} implementation for the {@code SECONDARY_HIT} update block.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class SecondaryHitUpdateBlock extends UpdateBlock {

    /**
     * Creates a new {@link SecondaryHitUpdateBlock}.
     */
    public SecondaryHitUpdateBlock() {
        super(UpdateFlag.SECONDARY_HIT);
    }

    @Override
    public void encodeForPlayer(Player player, ByteMessage msg) {
        Hit hit = unwrap(player.getSecondaryHit());
        msg.put(hit.getDamage());
        msg.put(hit.getType().getOpcode(), ByteTransform.S);
        msg.put(player.getCurrentHealth());
        msg.put(player.getTotalHealth(), ByteTransform.C);
    }

    @Override
    public void encodeForNpc(Npc npc, ByteMessage msg) {
        Hit hit = unwrap(npc.getSecondaryHit());
        msg.put(hit.getDamage(), ByteTransform.A);
        msg.put(hit.getType().getOpcode(), ByteTransform.C);
        msg.put(npc.getCurrentHealth(), ByteTransform.A);
        msg.put(npc.getTotalHealth());
    }

    @Override
    public int getPlayerMask() {
        return 512;
    }

    @Override
    public int getNpcMask() {
        return 8;
    }
}
