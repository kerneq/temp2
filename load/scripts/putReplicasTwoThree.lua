counter = 0
wrk.method = "PUT"

request = function()
    local path = "/v0/entity?id=" .. counter .. "&replicas=2/3"

    local body = ""
    for i = 1, 1024 do
        body = body .. string.char(math.random(32, 126))
    end

    wrk.body = body
    counter = counter + 1
    return wrk.format(nil, path)
end
