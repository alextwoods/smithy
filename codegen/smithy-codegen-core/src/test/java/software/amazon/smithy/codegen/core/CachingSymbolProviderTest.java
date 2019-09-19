package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

public class CachingSymbolProviderTest {
    @Test
    public void cachesResults() {
        List<ShapeId> calls = new ArrayList<>();

        SymbolProvider delegate = shape -> {
            calls.add(shape.getId());
            return Symbol.builder().name(shape.getId().getName()).build();
        };

        SymbolProvider cache = SymbolProvider.cache(delegate);

        Shape a = StringShape.builder().id("foo.baz#A").build();
        Shape b = StringShape.builder().id("foo.baz#B").build();
        Shape c = MemberShape.builder().id("foo.baz#C$c").target(a).build();

        assertThat(cache.toSymbol(a).getName(), equalTo("A"));
        assertThat(cache.toSymbol(b).getName(), equalTo("B"));
        assertThat(cache.toSymbol(c).getName(), equalTo("C"));
        assertThat(cache.toSymbol(a).getName(), equalTo("A"));
        assertThat(cache.toSymbol(b).getName(), equalTo("B"));
        assertThat(cache.toSymbol(c).getName(), equalTo("C"));
        assertThat(cache.toMemberName(c), equalTo("c"));

        assertThat(calls, containsInAnyOrder(a.getId(), b.getId(), c.getId()));
    }
}
