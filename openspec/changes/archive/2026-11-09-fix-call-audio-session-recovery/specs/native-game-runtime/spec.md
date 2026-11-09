## ADDED Requirements

### Requirement: Gameplay coexists with calls and other media
The runtime SHALL keep foreground gameplay running independently of audio ownership, mix with other media without interrupting its playback, and mute only its own music and sound effects while the system is in a ringing or communication mode. Ending a call SHALL restore game audio only while the game is foreground.

#### Scenario: Launch during a phone call
- **WHEN** the user starts a game during a call
- **THEN** the game renders and accepts input with its own audio muted
- **AND** the launch does not wait for audio focus or remain on a black screen

#### Scenario: Listen to music while playing
- **WHEN** the user opens a game while another app plays music, or starts music while playing
- **THEN** the game continues and mixes its sound with the other app without pausing that app

#### Scenario: Call begins or ends during gameplay
- **WHEN** the system enters ringing or communication mode while a game is foreground
- **THEN** game music and sound effects are muted without stopping gameplay
- **AND** ending the call restores the game's configured sound levels

#### Scenario: Call ends while game is backgrounded
- **WHEN** a call ends after the game has lost foreground ownership
- **THEN** gameplay and audio remain paused until the user returns

#### Scenario: Window focus returns during a call
- **WHEN** the game regains window focus while a call continues
- **THEN** game audio remains muted

### Requirement: Recovery of abandoned launch requests
The launcher SHALL recover abandoned pending and active sessions when their game process is absent, permit immediate retry, and preserve content and saves. Recovery SHALL preserve envelopes owned by a live game process or a launch being handed off to that process.

#### Scenario: Process dies before consuming a launch
- **WHEN** the launcher recovers a pending request with no owning or launching game process
- **THEN** it records one recoverable interruption result and permits a new launch without waiting for expiration

#### Scenario: Play overtakes queued recovery
- **WHEN** Play starts before previously queued recovery runs
- **THEN** old orphaned requests are recovered before the new request is created
- **AND** delayed recovery does not remove the new launch

#### Scenario: Native completion follows activity destruction
- **WHEN** native shutdown finishes after the activity has been destroyed
- **THEN** the private game process terminates and permits the next launch

#### Scenario: Multiple terminal paths run
- **WHEN** startup failure or interruption is followed by native completion
- **THEN** the first terminal result is preserved and consumed only once
