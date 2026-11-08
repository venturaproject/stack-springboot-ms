rootProject.name = "boilerplate-spring"

// The implemented monolith is the root project. These extraction shells are
// opt-in modules under the same build, so the default project remains backend/.
include(
    ":services:identity-service",
    ":services:customer-service",
    ":services:catalog-service",
    ":services:order-service",
    ":services:gateway-service",
)
