# LibreTube Architectural Decision Records (ADRs)

## ADR-001: Relocate Build Tools, JDK, and Gradle Cache to E: Drive
- **Context**: Drive C: has very limited free space (< 7 GB). Android SDK and Gradle caches can quickly exceed this.
- **Decision**: All build tools (JDK 17, Android SDK cmdline-tools, platform-tools) and `GRADLE_USER_HOME` have been permanently directed to `E:\`.
- **Status**: Implemented and verified.
