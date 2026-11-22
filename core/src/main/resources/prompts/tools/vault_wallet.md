Return the user's saved payout wallet, preferring the default wallet.

Use it before a withdrawal or when a site asks for a payout address. With no
`network`, return the user's default wallet (LTC unless they chose another).
If the request names a network, pass it and use only a wallet on that network.
Use the returned address exactly as stored; never invent, shorten, checksum or
substitute an address. If the needed network is not saved, stop and ask the
user rather than using a different network.
