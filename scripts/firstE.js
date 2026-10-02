// Nuclear Power Leak - First Event Handler
// Handles wave events

// Access the EventType class
const EventType = Packages.mindustry.game.EventType;

// Register wave event handler
// WaveEvent has a 'wave' property (the wave number), NOT 'class'
Events.on(EventType.WaveEvent, event => {
    print("[nuclear-power-leak] Wave " + event.wave + " started");
});

print("[nuclear-power-leak] firstE.js loaded");

// Rhino require() needs the script to return a value
return {};
