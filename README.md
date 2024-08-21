# Set Card Game

Welcome to the Set Card Game! This project implements the popular card game "Set," where players compete to identify sets of cards based on specific rules. The game is designed to be played on a computer with a graphical user interface, and it supports multiple players.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [How to Play](#how-to-play)
  - [Starting the Game](#starting-the-game)
  - [Game Rules](#game-rules)
  - [Commands](#commands)
- [Contributing](#contributing)
- [License](#license)

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or higher.
- An IDE or text editor (e.g., IntelliJ IDEA, Eclipse, VS Code).
- A terminal or command prompt.

### Installation

1. Clone the repository to your local machine:

   ```bash
   git clone https://github.com/yourusername/SetCardGame.git
   ```

2. Navigate to the project directory:

   ```bash
   cd SetCardGame
   ```

3. Compile the project:

   ```bash
   javac -d bin src/**/*.java
   ```

4. Run the game:

   ```bash
   java -cp bin bguspl.set.Main
   ```

## How to Play

### Starting the Game

1. Start the game by running the `Main` class.
2. The game window will appear, displaying the game board and the cards.

### Game Rules

- The goal of the game is to identify sets of three cards.
- Each card has four features: shape, color, number, and shading.
- A set consists of three cards where each feature is either all the same on each card or all different on each card.

### Commands

- Players can use the keyboard to select cards and declare sets.
- Each player is assigned specific keys to select cards on the table.

### Keyboard Controls

- Each player has specific keys mapped to select cards on the table.
- Press the corresponding keys to select or deselect cards.
- Once three cards are selected, the game will automatically check if they form a valid set.

---

Enjoy playing the Set Card Game! May your card-finding skills be sharp and your matches quick. If you have any questions or need assistance, feel free to reach out in the Issues section of the repository. Have fun!
