package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.MirrorOfTheVoidEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class MirrorOfTheVoid extends VestigeItem {

    public MirrorOfTheVoid(Properties props) {
        super(props, List.of(
                List.of(new MirrorOfTheVoidEffect(60)),
                List.of(new MirrorOfTheVoidEffect(45)),
                List.of(new MirrorOfTheVoidEffect(30))
        ), ModRarity.LEGENDARY);
    }
}

