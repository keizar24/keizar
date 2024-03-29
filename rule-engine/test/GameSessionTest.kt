package org.keizar.game

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.keizar.game.snapshot.buildGameSession
import org.keizar.game.snapshot.buildGameSnapshot
import org.keizar.utils.communication.game.BoardPos
import org.keizar.utils.communication.game.GameResult
import org.keizar.utils.communication.game.Player
import kotlin.test.assertContentEquals

class GameSessionTest {
    @Test
    fun `test random create`() = runTest {
        val game = GameSession.create()
        assertEquals(0, game.currentRoundNo.value)
        assertEquals(Role.WHITE, game.currentRole(Player.FirstWhitePlayer).value)
        assertEquals(Role.BLACK, game.currentRole(Player.FirstBlackPlayer).value)
    }

    @Test
    fun `test currentRole`() = runTest {
        val game = GameSession.create(0)
        assertEquals(Role.WHITE, game.currentRole(Player.FirstWhitePlayer).value)
        assertEquals(Role.BLACK, game.currentRole(Player.FirstBlackPlayer).value)
    }

    @Test
    fun `test currentRole, currentRoundNo and confirmNextRound`() = runTest {
        val game = GameSession.create(0)
        assertEquals(0, game.currentRoundNo.value)
        assertEquals(Role.WHITE, game.currentRole(Player.FirstWhitePlayer).value)
        assertEquals(Role.BLACK, game.currentRole(Player.FirstBlackPlayer).value)
        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(Role.BLACK, game.currentRole(Player.FirstWhitePlayer).value)
        assertEquals(Role.WHITE, game.currentRole(Player.FirstBlackPlayer).value)
    }

    @Test
    fun `test confirmNextRound and finalWinner`() = runTest {
        val game = GameSession.create(0)
        val round1 = game.currentRound.first()
        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("h7"), BoardPos("h6")))
        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round2 = game.currentRound.first()
        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("h7"), BoardPos("h6")))
        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))
        assertEquals(1, game.currentRoundNo.value)
        assertEquals(GameResult.Draw, game.finalWinner.first())
    }

    @Test
    fun `test confirmNextRound, winningCounter, wonRounds and getRoundWinner`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val round1 = curRound.first()
        assertEquals(null, round1.winner.value)
        assertEquals(0, round1.winningCounter.value)

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))
        assertEquals(Role.BLACK, round1.winner.value)
        assertEquals(3, round1.winningCounter.value)

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round2 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(0, round2.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())
        assertEquals(Player.FirstBlackPlayer, game.getRoundWinner(0).first())
        assertEquals(null, game.finalWinner.first())

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("h7"), BoardPos("h6")))
        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))
        assertEquals(GameResult.Winner(Player.FirstBlackPlayer), game.finalWinner.first())
    }

    @Test
    fun `test playable after next round`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val round1 = curRound.first()

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))
        assertEquals(Role.BLACK, round1.winner.value)
        assertEquals(3, round1.winningCounter.value)

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round2 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(0, round2.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round2.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round2.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round2.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round2.move(BoardPos("h4"), BoardPos("h5")))
        assertEquals(Role.BLACK, round2.winner.value)
        assertEquals(3, round2.winningCounter.value)

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(1, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())
        assertEquals(Player.FirstBlackPlayer, game.getRoundWinner(0).first())
        assertEquals(Player.FirstWhitePlayer, game.getRoundWinner(1).first())
    }

    @Test
    fun `test getSnapshot and restore`() = runTest {
        val game = GameSession.create(0)
        val round = game.currentRound.first()
        val pieces = round.pieces
        assertEquals(
            BoardPos.range("a1" to "h2").toSet() + BoardPos.range("a7" to "h8").toSet(),
            pieces.map { it.pos.value }.toSet()
        )

        assertTrue(round.move(BoardPos("a2"), BoardPos("a3")))
        assertEquals(
            setOf(
                "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1",
                "a3", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
            ).map { BoardPos.fromString(it) }.toSet(),
            pieces.filter { it.role == Role.WHITE }.map { it.pos.value }.toSet(),
        )

        val snapshot = game.getSnapshot()
        val newGame = GameSession.restore(snapshot)
        val newRound = game.currentRound.first()
        val newPieces = newRound.pieces
        assertEquals(snapshot, newGame.getSnapshot())

        assertTrue(newRound.move(BoardPos("e7"), BoardPos("e6")))
        assertTrue(newRound.move(BoardPos("a3"), BoardPos("a7")))

        assertEquals(
            setOf(
                "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1",
                "a7", "b2", "c2", "d2", "e2", "f2", "g2", "h2"
            ).map { BoardPos.fromString(it) }.toSet(),
            newPieces.filter { it.role == Role.WHITE && !it.isCaptured.value }.map { it.pos.value }
                .toSet(),
        )

        assertEquals(
            setOf(
                "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
                "b7", "c7", "d7", "e6", "f7", "g7", "h7",
            ).map { BoardPos.fromString(it) }.toSet(),
            newPieces.filter { it.role == Role.BLACK && !it.isCaptured.value }.map { it.pos.value }
                .toSet(),
        )

        assertEquals(0, newRound.getLostPiecesCount(Role.WHITE).value)
        assertEquals(1, newRound.getLostPiecesCount(Role.BLACK).value)

        assertTrue(newRound.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(newRound.move(BoardPos("a7"), BoardPos("b8")))

        assertEquals(0, newRound.getLostPiecesCount(Role.WHITE).value)
        assertEquals(2, newRound.getLostPiecesCount(Role.BLACK).value)
    }

    @Test
    fun `test getRole and getPlayer`() = runTest {
        val game = GameSession.create(0)
        val firstWhitePlayerRole = game.currentRole(Player.FirstWhitePlayer)
        assertEquals(Role.WHITE, firstWhitePlayerRole.value)
        assertEquals(firstWhitePlayerRole.value, game.getRole(Player.FirstWhitePlayer, 0))
        assertEquals(firstWhitePlayerRole.value.other(), game.getRole(Player.FirstBlackPlayer, 0))
        assertEquals(firstWhitePlayerRole.value.other(), game.getRole(Player.FirstWhitePlayer, 1))
        assertEquals(firstWhitePlayerRole.value, game.getRole(Player.FirstBlackPlayer, 1))

        for (roundNo in 0..1) {
            for (player in Player.entries) {
                assertEquals(player, game.getPlayer(game.getRole(player, roundNo), roundNo))
            }
        }
    }

    @Test
    fun `test replayCurrentRound`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val round1 = curRound.first()

        assertEquals(0, game.currentRoundNo.value)

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.replayCurrentRound())

        val round2 = curRound.first()

        assertEquals(0, game.currentRoundNo.value)

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round2.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round2.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round2.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round2.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round3 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)

        assertEquals(0, round3.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())
        assertEquals(Player.FirstBlackPlayer, game.getRoundWinner(0).first())
        assertEquals(null, game.finalWinner.first())

        assertTrue(round3.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round3.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round3.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round3.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round3.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round3.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round3.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round3.move(BoardPos("h7"), BoardPos("h6")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(GameResult.Winner(Player.FirstBlackPlayer), game.finalWinner.first())
    }

    @Test
    fun `test replayGame`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val round1 = curRound.first()

        assertEquals(0, game.currentRoundNo.value)

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round2 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(0, round2.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round2.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round2.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round2.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round2.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(1, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())
        assertEquals(Player.FirstBlackPlayer, game.getRoundWinner(0).first())
        assertEquals(Player.FirstWhitePlayer, game.getRoundWinner(1).first())

        assertTrue(game.replayGame())

        val round3 = curRound.first()

        assertEquals(0, game.currentRoundNo.value)

        assertTrue(round3.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round3.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round3.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round3.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round3.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round3.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round3.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round3.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round3.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round3.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round3.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round3.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round3.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round4 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(0, round4.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())

        assertTrue(round4.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round4.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round4.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round4.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round4.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round4.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round4.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round4.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round4.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round4.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round4.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round4.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round4.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(1, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())
        assertEquals(Player.FirstBlackPlayer, game.getRoundWinner(0).first())
        assertEquals(Player.FirstWhitePlayer, game.getRoundWinner(1).first())
    }

    @Test
    fun `I win when opponent has no piece to move`() = runTest {
        val gameSnapshot = buildGameSnapshot {
            properties {
                tiles {
                    change("c3" to TileType.QUEEN)
                }
            }

            val curRound = round {
                curRole { Role.BLACK }
                resetPieces {
                    white("a4")
                    black("a6")
                }
            }

            round {}
            setCurRound(curRound)
        }

        val game = GameSession.restore(gameSnapshot)
        val round = game.currentRound.first()

        assertTrue(round.move(BoardPos("a6"), BoardPos("a5")))
        assertEquals(Role.BLACK, round.winner.value)
    }

    @Test
    fun `I win when opponent has no piece to move (more pieces)`() = runTest {
        val game = buildGameSession {
            val curRound = round {
                curRole { Role.WHITE }
                resetPieces {
                    val c = 'a'
                    black("${c}8")
                    black("${c}7")
                    white("${c}6")
                    white("${c}4")
                }
            }

            round {}
            setCurRound(curRound)
        }

        val round = game.currentRound.first()

        assertTrue(round.move(BoardPos("a4"), BoardPos("a5")))
        assertEquals(Role.WHITE, round.winner.value)

        game.confirmNextRound(Player.FirstWhitePlayer)
        game.confirmNextRound(Player.FirstBlackPlayer)

        val newRound = game.currentRound.first()
        assertNotEquals(round, newRound)

    }

    @Test
    fun `test 2 round ends and it's a draw`() = runTest {
        val gameSnapshot = buildGameSnapshot {
            properties {
                tiles {
                    change("c3" to TileType.QUEEN)
                }
            }

            round {
                curRole { Role.BLACK }
                resetPieces {
                    add(Role.WHITE, BoardPos("a4"), isCaptured = false)
                    add(Role.BLACK, BoardPos("d6"), isCaptured = false)
                    // captured pieces
                    add(Role.WHITE, BoardPos("d6"), isCaptured = true)
                    add(Role.BLACK, BoardPos("a4"), isCaptured = true)
                }
                winner { Role.BLACK }
            }
            val round2 = round {
                curRole { Role.BLACK }
                resetPieces {
                    clear()
                    add(Role.WHITE, BoardPos("a4"), isCaptured = false)
                    add(Role.BLACK, BoardPos("d6"), isCaptured = false)
                    // captured pieces
                    add(Role.WHITE, BoardPos("d6"), isCaptured = true)
                    add(Role.BLACK, BoardPos("a4"), isCaptured = true)
                }
                winner { Role.BLACK }
            }
            setCurRound(round2)
        }

        val game = GameSession.restore(gameSnapshot)
        assertEquals(1, game.currentRoundNo.first())
        assertEquals(GameResult.Draw, game.finalWinner.first())
    }

    @Test
    fun `test first round finished in the middle of second round`() = runTest {
        val gameSnapshot = buildGameSnapshot {
            properties {
                tiles {
                    change("c3" to TileType.QUEEN)
                }
            }

            round {
                curRole { Role.BLACK }
                resetPieces {
                    clear()
                    add(Role.WHITE, BoardPos("a4"), isCaptured = false)
                    add(Role.BLACK, BoardPos("d6"), isCaptured = false)
                    // captured pieces
                    add(Role.WHITE, BoardPos("d6"), isCaptured = true)
                    add(Role.BLACK, BoardPos("a4"), isCaptured = true)
                }
                winner { Role.BLACK }
            }
            val round2 = round {}
            setCurRound(round2)
        }

        val game = GameSession.restore(gameSnapshot)
        // set current round to the second round
        assertEquals(1, game.currentRoundNo.first())
    }

    @Test
    fun `test game ends when no piece can move 2`() = runTest {
        val gameSnapshot = buildGameSnapshot {
            val curRound = round {
                curRole { Role.BLACK }
                resetPieces {
                    white("a8")
                    black("a2")
                }
            }
            round {}
            setCurRound(curRound)
        }

        val game = GameSession.restore(gameSnapshot)
        val round = game.currentRound.first()
        val whitePieces = round.getAllPiecesPos(Role.WHITE)
        val whiteMoves = whitePieces.flatMap { round.getAvailableTargets(it) }
        assertTrue(whiteMoves.isEmpty())
        assertTrue(round.move(BoardPos("a2"), BoardPos("a1")))
        assertEquals(Role.BLACK, round.winner.value)
    }

    @Test
    fun `test getRoundStats`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val round1 = curRound.first()

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))

        val stats = game.getRoundStats(0, Player.FirstWhitePlayer).first()
        assertEquals(Player.FirstWhitePlayer, stats.player)
        assertEquals(Player.FirstBlackPlayer, stats.winner)
        assertEquals(0, stats.neutralStats.whiteCaptured)
        assertEquals(1, stats.neutralStats.blackCaptured)
        assertEquals(6, stats.neutralStats.whiteMoves)
        assertEquals(7, stats.neutralStats.blackMoves)

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        val round2 = curRound.first()

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round2.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round2.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round2.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round2.move(BoardPos("h4"), BoardPos("h5")))
        assertEquals(Role.BLACK, round2.winner.value)
        assertEquals(3, round2.winningCounter.value)

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(1, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())
        assertEquals(Player.FirstBlackPlayer, game.getRoundWinner(0).first())
        assertEquals(Player.FirstWhitePlayer, game.getRoundWinner(1).first())
    }

    @Test
    fun `test playersConfirmedNextRound in snapshot`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val round1 = curRound.first()

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertContentEquals(
            setOf(Player.FirstWhitePlayer),
            game.playersConfirmedNextRound.value.asIterable()
        )

        val snapshot = game.getSnapshot()
        assertContentEquals(
            setOf(Player.FirstWhitePlayer),
            snapshot.playersConfirmedNextRound.asIterable()
        )

        val newGame = GameSession.restore(snapshot)
        assertTrue(newGame.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, newGame.currentRoundNo.value)
        assertContentEquals(
            emptySet(),
            newGame.playersConfirmedNextRound.value.asIterable()
        )
    }

    @Test
    fun `test finalWinner flow behaviour`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val p1WonRounds = game.wonRounds(Player.FirstWhitePlayer)
        val p2WonRounds = game.wonRounds(Player.FirstBlackPlayer)
        val p1LostPieces = game.lostPieces(Player.FirstWhitePlayer)
        val p2LostPieces = game.lostPieces(Player.FirstBlackPlayer)
        val finalWinner = game.finalWinner
        val round1 = curRound.first()

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h6")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h6"), BoardPos("h7")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(0, p1WonRounds.first())
        assertEquals(1, p2WonRounds.first())
        assertEquals(1, p1LostPieces.first())
        assertEquals(1, p2LostPieces.first())
        assertEquals(null, finalWinner.first())

        val round2 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(0, round2.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round2.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round2.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round2.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round2.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, p1WonRounds.first())
        assertEquals(1, p2WonRounds.first())
        assertEquals(1, p1LostPieces.first())
        assertEquals(2, p2LostPieces.first())
        assertEquals(GameResult.Winner(Player.FirstWhitePlayer), finalWinner.first())
    }

    @Test
    fun `test finalWinner flow behaviour 2`() = runTest {
        val game = GameSession.create(0)
        val curRound = game.currentRound
        val p1WonRounds = game.wonRounds(Player.FirstWhitePlayer)
        val p2WonRounds = game.wonRounds(Player.FirstBlackPlayer)
        val p1LostPieces = game.lostPieces(Player.FirstWhitePlayer)
        val p2LostPieces = game.lostPieces(Player.FirstBlackPlayer)
        val finalWinner = game.finalWinner
        val round1 = curRound.first()

        assertTrue(round1.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round1.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round1.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round1.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round1.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round1.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round1.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round1.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round1.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round1.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round1.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round1.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(0, p1WonRounds.first())
        assertEquals(1, p2WonRounds.first())
        assertEquals(1, p1LostPieces.first())
        assertEquals(0, p2LostPieces.first())
        assertEquals(null, finalWinner.first())

        val round2 = curRound.first()

        assertEquals(1, game.currentRoundNo.value)
        assertEquals(0, round2.winningCounter.value)
        assertEquals(0, game.wonRounds(Player.FirstWhitePlayer).first())
        assertEquals(1, game.wonRounds(Player.FirstBlackPlayer).first())

        assertTrue(round2.move(BoardPos("e2"), BoardPos("e3")))
        assertTrue(round2.move(BoardPos("d7"), BoardPos("d6")))
        assertTrue(round2.move(BoardPos("e3"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("a7"), BoardPos("a6")))
        assertTrue(round2.move(BoardPos("a2"), BoardPos("a3")))
        assertTrue(round2.move(BoardPos("a6"), BoardPos("a5")))
        assertTrue(round2.move(BoardPos("a3"), BoardPos("a4")))
        assertTrue(round2.move(BoardPos("d6"), BoardPos("d5")))
        assertTrue(round2.move(BoardPos("h2"), BoardPos("h3")))
        assertTrue(round2.move(BoardPos("f7"), BoardPos("f6")))
        assertTrue(round2.move(BoardPos("h3"), BoardPos("h4")))
        assertTrue(round2.move(BoardPos("f6"), BoardPos("f5")))
        assertTrue(round2.move(BoardPos("h4"), BoardPos("h5")))

        assertTrue(game.confirmNextRound(Player.FirstWhitePlayer))
        assertTrue(game.confirmNextRound(Player.FirstBlackPlayer))

        assertEquals(1, p1WonRounds.first())
        assertEquals(1, p2WonRounds.first())
        assertEquals(1, p1LostPieces.first())
        assertEquals(1, p2LostPieces.first())
        assertEquals(GameResult.Draw, finalWinner.first())
    }
}