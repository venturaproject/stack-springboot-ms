import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import InputError from './InputError'

describe('InputError', () => {
  it('renders the message when given one', () => {
    render(<InputError message="Email is required" />)
    expect(screen.getByText('Email is required')).toBeInTheDocument()
  })

  it('renders nothing when there is no message', () => {
    const { container } = render(<InputError />)
    expect(container).toBeEmptyDOMElement()
  })
})
