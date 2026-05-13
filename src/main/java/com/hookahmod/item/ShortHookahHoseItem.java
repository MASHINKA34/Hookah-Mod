package com.hookahmod.item;

public class ShortHookahHoseItem extends HookahHoseItem {
    public ShortHookahHoseItem(Properties props) { super(props); }

    @Override
    public HookahHoseType getHoseType() { return HookahHoseType.SHORT; }
}
