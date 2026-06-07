# Multi-Agents JADE — Workshop Simulation

Academic project (M1) — a multi-agent system simulation built with the [JADE](https://jade.tilab.com/) framework.

The simulation models a workshop where autonomous robot agents process products based on their skill sets, with configurable production parameters.

## Agents

| Agent | Role |
|---|---|
| `Robot` | Processes products using its assigned skills |
| `Atelier` | Manages the product queue and dispatches tasks |
| `BaseAgent` | Shared base class for all agents |

## Configuration

All parameters are set in `start_project.sh`:

| Variable | Description |
|---|---|
| `NB_ROBOTS` | Number of robot agents |
| `LAMBDA1`, `LAMBDA2` | Production delays |
| `LAMBDA3` | Processing time |
| `NB_COMPETENCES` | Total number of skills |

## Run

```bash
chmod +x start_project.sh
./start_project.sh
```

## Requirements

- Java 17+
- JADE (`lib/jade.jar` included)

## Authors

Tom David, Titouan Pasquier, Marius Guillais, Maxim Chepy
