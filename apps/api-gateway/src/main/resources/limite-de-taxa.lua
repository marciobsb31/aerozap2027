-- Limite de taxa por janela fixa.
-- KEYS[1] = chave da origem na janela corrente
-- ARGV[1] = limite de requisições    ARGV[2] = tamanho da janela, em segundos
-- Retorna "permitido;restantes;segundos_ate_reiniciar" (1 = permitido).
--
-- Contar e marcar o vencimento no mesmo script evita contador eterno: se o processo morresse
-- entre o INCR e o EXPIRE, a origem ficaria bloqueada para sempre.

local limite = tonumber(ARGV[1])
local janela = tonumber(ARGV[2])

local atual = redis.call('INCR', KEYS[1])
if atual == 1 then
  redis.call('EXPIRE', KEYS[1], janela)
end

local restante = redis.call('TTL', KEYS[1])
if restante < 0 then
  restante = janela
  redis.call('EXPIRE', KEYS[1], janela)
end

local sobrando = limite - atual
if sobrando < 0 then
  sobrando = 0
end

local permitido = 1
if atual > limite then
  permitido = 0
end

return tostring(permitido) .. ';' .. tostring(sobrando) .. ';' .. tostring(restante)
