package com.boilerplate.infrastructure.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.application.dispatch.Command;
import com.boilerplate.application.dispatch.CommandHandler;
import com.boilerplate.application.dispatch.Query;
import com.boilerplate.application.dispatch.QueryHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpringBusTest {

    @Test
    void command_bus_dispatches_to_the_handler_registered_for_the_concrete_command_type() {
        SpringCommandBus bus = new SpringCommandBus(List.of(new EchoCommandHandler()));

        assertThat(bus.dispatch(new EchoCommand("write"))).isEqualTo("handled: write");
    }

    @Test
    void command_bus_fails_fast_when_no_handler_exists() {
        SpringCommandBus bus = new SpringCommandBus(List.of());

        assertThatThrownBy(() -> bus.dispatch(new EchoCommand("write")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(EchoCommand.class.getName());
    }

    @Test
    void command_bus_rejects_ambiguous_handler_registrations() {
        assertThatThrownBy(() -> new SpringCommandBus(List.of(new EchoCommandHandler(), new EchoCommandHandler())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EchoCommand.class.getName());
    }

    @Test
    void query_bus_dispatches_to_the_handler_registered_for_the_concrete_query_type() {
        SpringQueryBus bus = new SpringQueryBus(List.of(new EchoQueryHandler()));

        assertThat(bus.dispatch(new EchoQuery("read"))).isEqualTo("handled: read");
    }

    @Test
    void query_bus_fails_fast_when_no_handler_exists() {
        SpringQueryBus bus = new SpringQueryBus(List.of());

        assertThatThrownBy(() -> bus.dispatch(new EchoQuery("read")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(EchoQuery.class.getName());
    }

    @Test
    void query_bus_rejects_ambiguous_handler_registrations() {
        assertThatThrownBy(() -> new SpringQueryBus(List.of(new EchoQueryHandler(), new EchoQueryHandler())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EchoQuery.class.getName());
    }

    private record EchoCommand(String value) implements Command<String> {}

    private record EchoQuery(String value) implements Query<String> {}

    private static final class EchoCommandHandler implements CommandHandler<EchoCommand, String> {

        @Override
        public Class<EchoCommand> commandType() {
            return EchoCommand.class;
        }

        @Override
        public String handle(EchoCommand command) {
            return "handled: " + command.value();
        }
    }

    private static final class EchoQueryHandler implements QueryHandler<EchoQuery, String> {

        @Override
        public Class<EchoQuery> queryType() {
            return EchoQuery.class;
        }

        @Override
        public String handle(EchoQuery query) {
            return "handled: " + query.value();
        }
    }
}
