package artifacts.registry;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RegistryHolder<R, V extends R> implements Supplier<V> {

    private final ResourceKey<R> key;
    private final Supplier<V> factory;
    private Holder<R> holder;

    public RegistryHolder(ResourceKey<R> key, Supplier<V> factory) {
        this.key = key;
        this.factory = factory;
        this.holder = Holder.Reference.createStandAlone(new HolderOwner<>() {
            @Override
            public boolean canSerializeIn(HolderOwner<R> holderOwner) {
                return true;
            }
        }, key);
    }

    public Supplier<V> getFactory() {
        return factory;
    }

    public void bind(Holder<R> holder) {
        if (this.holder.isBound()) {
            throw new IllegalStateException();
        }
        bindValue(holder.value());
    }

    private void bindValue(R value) {
        try {
            var method = Holder.Reference.class.getDeclaredMethod("bindValue", Object.class);
            method.setAccessible(true);
            method.invoke(this.holder, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to bind registry holder", e);
        }
    }

    public Holder<R> holder() {
        return holder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() {
        return (V) value();
    }

    public R value() {
        return holder.value();
    }

    public boolean isBound() {
        return holder != null && holder.isBound();
    }

    public boolean areComponentsBound() {
        return isBound() && holder.areComponentsBound();
    }

    public boolean is(Identifier resourceLocation) {
        return resourceLocation.equals(key.identifier());
    }

    public boolean is(ResourceKey<R> resourceKey) {
        return resourceKey.equals(key);
    }

    public boolean is(Predicate<ResourceKey<R>> predicate) {
        return predicate.test(key);
    }

    public boolean is(TagKey<R> tagKey) {
        return isBound() && holder.is(tagKey);
    }

    @SuppressWarnings("deprecation")
    public boolean is(Holder<R> holder) {
        return isBound() && this.holder.is(holder);
    }

    public Stream<TagKey<R>> tags() {
        return isBound() ? holder.tags() : Stream.empty();
    }

    public DataComponentMap components() {
        return isBound() ? holder.components() : DataComponentMap.EMPTY;
    }

    public Either<ResourceKey<R>, R> unwrap() {
        return Either.left(key);
    }

    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(key);
    }

    public Holder.Kind kind() {
        return Holder.Kind.REFERENCE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Holder<?> h && h.kind() == Holder.Kind.REFERENCE && h.unwrapKey().isPresent() && h.unwrapKey().get() == this.key;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
