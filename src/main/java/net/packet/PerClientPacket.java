package net.packet;

import client.Client;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PerClientPacket implements Packet {
    private final Function<Client, Packet> factory;
    private final Supplier<Packet> fallback;

    public PerClientPacket(Function<Client, Packet> factory, Supplier<Packet> fallback) {
        this.factory = Objects.requireNonNull(factory);
        this.fallback = Objects.requireNonNull(fallback);
    }

    public Packet forClient(Client client) {
        return factory.apply(Objects.requireNonNull(client));
    }

    @Override
    public byte[] getBytes() {
        return fallback.get().getBytes();
    }
}
